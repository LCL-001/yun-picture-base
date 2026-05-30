package com.lcl.yunpicturebackend.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yunpicturebackend.domain.po.Post;
import com.lcl.yunpicturebackend.domain.po.PostLike;
import com.lcl.yunpicturebackend.exception.BusinessException;
import com.lcl.yunpicturebackend.exception.ErrorCode;
import com.lcl.yunpicturebackend.mapper.PostLikeMapper;
import com.lcl.yunpicturebackend.service.IPostLikeService;
import com.lcl.yunpicturebackend.service.IPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl extends ServiceImpl<PostLikeMapper, PostLike> implements IPostLikeService {

    private final StringRedisTemplate stringRedisTemplate;
    private final IPostService postService;

    private static final String LIKE_COUNT_KEY = "post:like:count:";
    private static final String LIKE_USERS_KEY = "post:like:users:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long postId, Long userId) {
        if (ObjectUtil.hasEmpty(postId, userId)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<PostLike> w = new LambdaQueryWrapper<>();
        w.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
        PostLike exist = this.getOne(w);
        if (exist != null) {
            this.removeById(exist.getId());
            stringRedisTemplate.opsForSet().remove(LIKE_USERS_KEY + postId, String.valueOf(userId));
            stringRedisTemplate.opsForValue().decrement(LIKE_COUNT_KEY + postId);
            updatePostLikeCount(postId, -1);
            return false;
        }
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        this.save(like);
        stringRedisTemplate.opsForSet().add(LIKE_USERS_KEY + postId, String.valueOf(userId));
        stringRedisTemplate.opsForValue().increment(LIKE_COUNT_KEY + postId);
        updatePostLikeCount(postId, 1);
        return true;
    }

    private void updatePostLikeCount(Long postId, int delta) {
        Post post = postService.getById(postId);
        if (post != null) {
            post.setLikeCount(Math.max(0, (post.getLikeCount() == null ? 0 : post.getLikeCount()) + delta));
            postService.updateById(post);
        }
    }

    @Override
    public long getLikeCount(Long postId) {
        String c = stringRedisTemplate.opsForValue().get(LIKE_COUNT_KEY + postId);
        if (c != null) return Long.parseLong(c);
        long count = this.count(new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, postId));
        stringRedisTemplate.opsForValue().set(LIKE_COUNT_KEY + postId, String.valueOf(count));
        return count;
    }

    @Override
    public boolean isLiked(Long postId, Long userId) {
        if (ObjectUtil.hasEmpty(postId, userId)) return false;
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(LIKE_USERS_KEY + postId, String.valueOf(userId)));
    }
}
