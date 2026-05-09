package com.lcl.yupicture.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yupicture.domain.space.service.SpaceDomainService;
import com.lcl.yupicture.infrastructure.common.DeleteRequest;
import com.lcl.yupicture.interfaces.assembler.SpaceAssembler;
import com.lcl.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import com.lcl.yupicture.domain.space.entity.Space;
import com.lcl.yupicture.domain.space.entity.SpaceUser;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.interfaces.vo.space.SpaceVO;
import com.lcl.yupicture.interfaces.vo.user.UserVO;
import com.lcl.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.lcl.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.lcl.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.lcl.yupicture.infrastructure.exception.BusinessException;
import com.lcl.yupicture.infrastructure.exception.ErrorCode;
import com.lcl.yupicture.infrastructure.exception.ThrowUtils;
import com.lcl.yupicture.infrastructure.mapper.SpaceMapper;
import com.lcl.yupicture.application.service.SpaceApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yupicture.application.service.SpaceUserApplicationService;
import com.lcl.yupicture.application.service.UserApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <p>
 * 空间 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-04-24
 */
@Service
@RequiredArgsConstructor
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceApplicationService {
    @Resource
    private SpaceDomainService spaceDomainService;
    private final UserApplicationService userApplicationService;
    private final TransactionTemplate transactionTemplate;
    private final SpaceUserApplicationService spaceUserApplicationService;
//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;
    private final ConcurrentHashMap<Long, Object> lockMap = new ConcurrentHashMap<>();// 锁对象

    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        // 将实体类和 DTO 进行转换
        Space spaceEntity = SpaceAssembler.toSpaceEntity(spaceAddRequest);
        // 设置默认值（空间名称和空间等级）
        if (StrUtil.isBlank(spaceEntity.getSpaceName())) {
            spaceEntity.setSpaceName("默认空间");
        }
        if (spaceEntity.getSpaceLevel() == null) {
            spaceEntity.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());// 默认普通版
        }
        if (spaceEntity.getSpaceType() == null) {
            spaceEntity.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());// 默认私有空间
        }
        // 填充数据
        this.fillSpaceBySpaceLevel(spaceEntity);
        // 数据校验
        spaceEntity.validSpace(true);
        // 权限校验
        Long userId = loginUser.getId();
        spaceEntity.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != spaceEntity.getSpaceLevel() && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别空间");
        }
        // 针对用户加锁
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            try {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType, spaceEntity.getSpaceType())
                            .exists();
                    ThrowUtils.throwIf(exists && SpaceTypeEnum.PRIVATE.getValue() == spaceEntity.getSpaceType(), ErrorCode.OPERATION_ERROR, "每个用户私有空间仅能有一个");
                    // 写入数据库
                    boolean save = this.save(spaceEntity);
                    ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "创建空间失败，向数据库添加数据失败");
                    // 创建成功后，如果是团队空间，则关联新增团队成员记录
                    if (SpaceTypeEnum.TEAM.getValue() == spaceEntity.getSpaceType()) {
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(spaceEntity.getId());
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        boolean success = spaceUserApplicationService.save(spaceUser);
                        ThrowUtils.throwIf(!success, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败，向数据库添加数据失败");
                    }
//                    // 创建分表
//                    dynamicShardingManager.createSpacePictureTable(space);
                    // 返回新写入的空间 id
                    return spaceEntity.getId();
                });
                // 返回结果是包装类，可以做一些处理
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } finally {
                // 移除锁, 防止内存泄漏
                lockMap.remove(userId);
            }
        }
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userApplicationService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    @Override
    public void deleteSpace(DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Space oldSpace = getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        this.checkSpaceAuth(oldSpace, loginUser);
        // 操作数据库
        boolean result = removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void checkSpaceAuth(Space oldSpace, User loginUser) {
        spaceDomainService.checkSpaceAuth(oldSpace, loginUser);
    }

    @Override
    public void updateSpace(SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        spaceDomainService.updateSpace(spaceUpdateRequest, request);
    }

    @Override
    public void editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Space spaceEntity = SpaceAssembler.toSpaceEntity(spaceEditRequest);
        // 设置编辑时间
        spaceEntity.setEditTime(new Date());
        // 自动填充数据
        this.fillSpaceBySpaceLevel(spaceEntity);
        // 数据校验
        spaceEntity.validSpace(false);
        User loginUser = userApplicationService.getLoginUser(request);
        // 判断是否存在
        long id = spaceEditRequest.getId();
        Space oldSpace = this.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        this.checkSpaceAuth(oldSpace, loginUser);
        // 操作数据库
        boolean result = this.updateById(spaceEntity);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }
}
