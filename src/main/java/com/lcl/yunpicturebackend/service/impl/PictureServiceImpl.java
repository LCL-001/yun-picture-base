package com.lcl.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lcl.yunpicturebackend.common.DeleteRequest;
import com.lcl.yunpicturebackend.common.ResultUtils;
import com.lcl.yunpicturebackend.domain.dto.file.UploadPictureResult;
import com.lcl.yunpicturebackend.domain.dto.picture.*;
import com.lcl.yunpicturebackend.domain.po.Picture;
import com.lcl.yunpicturebackend.domain.po.User;
import com.lcl.yunpicturebackend.domain.vo.PictureVO;
import com.lcl.yunpicturebackend.domain.vo.UserVO;
import com.lcl.yunpicturebackend.enums.PictureReviewStatusEnum;
import com.lcl.yunpicturebackend.exception.BusinessException;
import com.lcl.yunpicturebackend.exception.ErrorCode;
import com.lcl.yunpicturebackend.exception.ThrowUtils;
import com.lcl.yunpicturebackend.manager.upload.FilePictureUpload;
import com.lcl.yunpicturebackend.manager.upload.PictureUploadTemplate;
import com.lcl.yunpicturebackend.manager.upload.URLFilePictureUpload;
import com.lcl.yunpicturebackend.mapper.PictureMapper;
import com.lcl.yunpicturebackend.service.IPictureService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yunpicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 图片 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-04-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements IPictureService {

    private final IUserService userService;
    private final FilePictureUpload pictureUpload;
    private final URLFilePictureUpload urlFilePictureUpload;
    private final StringRedisTemplate stringRedisTemplate;
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite((long) (5 + Math.random() * 5), TimeUnit.MINUTES)
                    .build();


    @Override
    @Transactional
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 判断用户是否拥有权限
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是新增图片还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，则需要判断图片是否存在
        if (pictureId != null) {
            Picture picture = this.getById(pictureId);
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            ThrowUtils.throwIf(!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        }
        // 上传图片，获取信息
        // 按用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate;
        if (inputSource instanceof MultipartFile) {
            pictureUploadTemplate = pictureUpload;
        } else {
            pictureUploadTemplate = urlFilePictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要上传的图片信息
        Picture picture = getPicture(loginUser, uploadPictureResult, pictureUploadRequest, pictureId);
        // 填充审核信息
        fillReviewParams(picture, loginUser);
        boolean save = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "图片上传失败");
        // 清除缓存
        this.clearPictureListCache();
        return PictureVO.objToVo(picture);
    }

    @Override
    @Transactional
    public int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 获取搜索词
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 获取图片名称前缀
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 校验抓取图片数量，不能超过30张
        ThrowUtils.throwIf(pictureUploadByBatchRequest.getCount() > 30, ErrorCode.PARAMS_ERROR, "图片数量不能超过30张");
        // 设置要抓取的地址，目前是写死的（todo 可动态更换抓取网址）
        String fetchURL = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        // 使用 jsoup 抓取图片
        Document document;
        try {
            // 获取页面
            document = Jsoup.connect(fetchURL).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取页面失败");
        }
        // 获取图片元素所在 div 元素
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取元素失败");
        }
        // 获取图片元素
        //Elements imgElementList = div.select("img.mimg");
        Elements imgElementList = div.select(".iusc");  // 修改选择器，获取包含完整数据的元素
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
//            String fileURL = imgElement.attr("src");
            // 获取data-m属性中的JSON字符串
            String dataM = imgElement.attr("m");
            String fileURL;
            try {
                // 解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                // 获取murl字段（原始图片URL）
                fileURL = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("解析图片数据失败", e);
                continue;
            }
            if (StrUtil.isBlank(fileURL)) {
                // 跳过无效图片
                log.info("当前链接为空，已跳过：{}", fileURL);
                continue;
            }
            // 处理图片链接的地址，防止出现转义错误
            int questionMarkIndex = fileURL.indexOf("?");
            if (questionMarkIndex > -1) {
                // 如果有参数，则截取掉
                fileURL = fileURL.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                // 设置图片名称，按序号连续递增命名
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileURL, pictureUploadRequest, loginUser);
                log.info("上传图片成功：{}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("上传图片失败：{}", e.getMessage());
                continue;
            }
            if (uploadCount >= pictureUploadByBatchRequest.getCount()) {
                break;
            }
        }
        return uploadCount;
    }

    private static Picture getPicture(User loginUser, UploadPictureResult uploadPictureResult, PictureUploadRequest pictureUploadRequest, Long pictureId) {
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果pictureId不为null，则是更新图片，否则是新增图片
        if (pictureId != null) {
            // 更新图片还要设置修改时间，补充id
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        return picture;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    @Transactional
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatus == null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断参数是否存在
        Picture oldPicture = this.getById(id);
        if (oldPicture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 已是该状态，则不能修改
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean success = this.updateById(updatePicture);
        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR);
        // 清除缓存
        this.clearPictureListCache();
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 如果是管理员，自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
        } else {
            // 否则，创建或编辑图片都要设置为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Page<PictureVO> listPictureVOByPageByCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();// 当前页
        long size = pictureQueryRequest.getPageSize();// 每页大小
        // 限制爬虫
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已经过审的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 构建缓存key
        // 将查询条件中的非关键参数排除，减少缓存Key的种类
        // 例如：current、pageSize 不应该影响缓存Key
        String hashKey = DigestUtils.md5DigestAsHex(
                JSONUtil.toJsonStr(buildCacheKey(pictureQueryRequest)).getBytes()
        );
        String key = String.format("yupicture:listPictureVOByPage:%s", hashKey);
        // 先从本地缓存 Caffeine 中获取
        String cachedValue = LOCAL_CACHE.getIfPresent(key);
        if (StringUtils.isNotBlank(cachedValue)) {
            // 如果命中缓存，返回结果
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
            return pictureVOPage;
        }
        // 本地缓存中没有，再从分布式缓存（Redis）中获取
        cachedValue = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(cachedValue)) {
            // 回写本地缓存
            LOCAL_CACHE.put(key, cachedValue);
            // 如果命中缓存，返回结果
            Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
            return pictureVOPage;
        }
        // 缓存都没有，使用分布式锁防止缓存击穿
        String lockKey = "yupicture:lock:" + hashKey;
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lock)) {
            // 获取锁成功
            try {
                // 双重检查：获取锁后再检查一次缓存
                cachedValue = stringRedisTemplate.opsForValue().get(key);
                if (StringUtils.isNotBlank(cachedValue)) {
                    // 回写本地缓存
                    LOCAL_CACHE.put(key, cachedValue);
                    // 如果命中缓存，返回结果
                    Page<PictureVO> pictureVOPage = JSONUtil.toBean(cachedValue, Page.class);
                    return pictureVOPage;
                }
                // 查询数据库
                Page<Picture> picturePage = page(new Page<>(current, size), getQueryWrapper(pictureQueryRequest));

                // 获取封装类
                Page<PictureVO> pictureVOPage = getPictureVOPage(picturePage, request);
                // 写入本地缓存
                String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
                LOCAL_CACHE.put(key, cacheValue);
                // 写入分布式缓存
                // 设置过期时间 5 ~ 10 分钟随机过期，防止缓存雪崩
                int cacheExpireTime;
                if (pictureVOPage.getRecords() == null || pictureVOPage.getRecords().isEmpty()) {
                    cacheExpireTime = 60; // 空结果缓存1分钟
                    log.info("空结果缓存，key: {}, 过期时间: {}s", key, cacheExpireTime);
                } else {
                    cacheExpireTime = 300 + RandomUtil.randomInt(0, 300); // 正常结果5-10分钟
                }
                stringRedisTemplate.opsForValue().set(key, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
                return pictureVOPage;
            } finally {
                // 释放锁
                stringRedisTemplate.delete(lockKey);
            }
        } else {
            // 获取锁失败，等待重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 直接从Redis获取，不再递归
            cachedValue = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(cachedValue)) {
                LOCAL_CACHE.put(key, cachedValue);
                return JSONUtil.toBean(cachedValue, Page.class);
            }
            // 如果还是没有，返回空结果或再次尝试（限制最大重试次数）
            return new Page<>();
        }
    }

    // ... existing code ...

    private PictureQueryRequest buildCacheKey(PictureQueryRequest request) {
        // 只保留影响查询结果的字段，排除分页参数（current、pageSize）
        PictureQueryRequest cacheKey = new PictureQueryRequest();
        cacheKey.setReviewStatus(request.getReviewStatus());
        cacheKey.setCategory(request.getCategory());
        cacheKey.setTags(request.getTags());
        cacheKey.setUserId(request.getUserId());
        cacheKey.setName(request.getName());
        cacheKey.setIntroduction(request.getIntroduction());
        cacheKey.setSearchText(request.getSearchText());
        cacheKey.setPicFormat(request.getPicFormat());
        cacheKey.setSortField(request.getSortField());
        cacheKey.setSortOrder(request.getSortOrder());
        return cacheKey;
    }

// ... existing code ...


    private void clearPictureListCache() {
        // 清除Redis缓存（使用通配符删除）
        Set<String> keys = stringRedisTemplate.keys("yupicture:listPictureVOByPage:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Override
    @Transactional
    public void deletePicture(DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Picture oldPicture = getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 清除缓存
        clearPictureListCache();
    }

    @Override
    @Transactional
    public void updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 清除缓存
        this.clearPictureListCache();
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 清除缓存
        this.clearPictureListCache();
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

}
