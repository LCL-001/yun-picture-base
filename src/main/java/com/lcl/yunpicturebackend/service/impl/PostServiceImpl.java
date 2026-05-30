package com.lcl.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yunpicturebackend.domain.dto.post.PostAddRequest;
import com.lcl.yunpicturebackend.domain.dto.post.PostEditRequest;
import com.lcl.yunpicturebackend.domain.dto.post.PostQueryRequest;
import com.lcl.yunpicturebackend.domain.po.Post;
import com.lcl.yunpicturebackend.domain.po.User;
import com.lcl.yunpicturebackend.domain.vo.PostVO;
import com.lcl.yunpicturebackend.domain.vo.UserVO;
import com.lcl.yunpicturebackend.exception.BusinessException;
import com.lcl.yunpicturebackend.exception.ErrorCode;
import com.lcl.yunpicturebackend.exception.ThrowUtils;
import com.lcl.yunpicturebackend.mapper.PostMapper;
import com.lcl.yunpicturebackend.service.IPostService;
import com.lcl.yunpicturebackend.service.ISocialService;
import com.lcl.yunpicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    private final IUserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    @Lazy
    private final ISocialService socialService;

    private static final String POST_VIEW_KEY = "post:view:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long publishPost(PostAddRequest request, Long userId) {
        ThrowUtils.throwIf(request == null || StrUtil.isBlank(request.getTitle()), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(request.getContent()), ErrorCode.PARAMS_ERROR, "内容不能为空");
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImages(request.getImages());
        post.setTags(request.getTags());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setStatus(0);
        this.save(post);
        socialService.pushToFollowersTimeline(userId, post.getId());
        return post.getId();
    }

    @Override
    public PostVO getPostVO(Post post, HttpServletRequest request) {
        if (post == null) return null;
        PostVO vo = PostVO.objToVo(post);
        // tags 转为列表
        if (StrUtil.isNotBlank(post.getTags())) {
            vo.setTagList(Arrays.asList(post.getTags().split(",")));
        }
        // 关联用户
        Long userId = post.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            vo.setUser(userService.getUserVO(user));
        }
        return vo;
    }

    @Override
    public Page<PostVO> getPostVOPage(PostQueryRequest request, HttpServletRequest httpRequest) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 0);
        if (request.getUserId() != null) {
            wrapper.eq(Post::getUserId, request.getUserId());
        }
        if (StrUtil.isNotBlank(request.getKeyword())) {
            wrapper.and(w -> w.like(Post::getTitle, request.getKeyword())
                            .or().like(Post::getContent, request.getKeyword()));
        }
        wrapper.orderByDesc(Post::getCreateTime);
        Page<Post> page = this.page(new Page<>(request.getCurrent(), request.getPageSize()), wrapper);

        List<PostVO> voList = page.getRecords().stream()
                .map(p -> getPostVO(p, httpRequest))
                .collect(Collectors.toList());

        Page<PostVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public PostVO getPostVOById(Long id, HttpServletRequest request) {
        Post post = this.getById(id);
        ThrowUtils.throwIf(post == null || post.getStatus() != 0, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        try {
            User loginUser = userService.getLoginUser(request);
            String key = POST_VIEW_KEY + id + ":" + loginUser.getId();
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
                post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
                this.updateById(post);
                stringRedisTemplate.opsForValue().set(key, "1");
            }
        } catch (Exception ignored) {
            // 未登录用户正常计数
            post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
            this.updateById(post);
        }
        return getPostVO(post, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPost(PostEditRequest request, Long userId) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        Post post = this.getById(request.getId());
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!post.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "只能编辑自己的帖子");
        if (StrUtil.isNotBlank(request.getTitle())) post.setTitle(request.getTitle());
        if (StrUtil.isNotBlank(request.getContent())) post.setContent(request.getContent());
        if (request.getImages() != null) post.setImages(request.getImages());
        if (request.getTags() != null) post.setTags(request.getTags());
        this.updateById(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id, Long userId) {
        Post post = this.getById(id);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!post.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "只能删除自己的帖子");
        this.removeById(id);
    }
}
