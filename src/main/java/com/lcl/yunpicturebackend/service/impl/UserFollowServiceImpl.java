package com.lcl.yunpicturebackend.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yunpicturebackend.domain.po.UserFollow;
import com.lcl.yunpicturebackend.exception.BusinessException;
import com.lcl.yunpicturebackend.exception.ErrorCode;
import com.lcl.yunpicturebackend.mapper.UserFollowMapper;
import com.lcl.yunpicturebackend.service.IUserFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements IUserFollowService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String FOLLOWING_KEY = "user:following:";
    private static final String FOLLOWING_COUNT_KEY = "user:following:count:";
    private static final String FOLLOWER_COUNT_KEY = "user:follower:count:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFollow(Long followerId, Long followeeId) {
        if (ObjectUtil.hasEmpty(followerId, followeeId) || followerId.equals(followeeId))
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能关注自己");
        LambdaQueryWrapper<UserFollow> w = new LambdaQueryWrapper<>();
        w.eq(UserFollow::getFollowerId, followerId).eq(UserFollow::getFolloweeId, followeeId);
        UserFollow exist = this.getOne(w);
        if (exist != null) {
            this.removeById(exist.getId());
            stringRedisTemplate.opsForSet().remove(FOLLOWING_KEY + followerId, String.valueOf(followeeId));
            stringRedisTemplate.opsForValue().decrement(FOLLOWING_COUNT_KEY + followerId);
            stringRedisTemplate.opsForValue().decrement(FOLLOWER_COUNT_KEY + followeeId);
            return false;
        }
        UserFollow f = new UserFollow();
        f.setFollowerId(followerId);
        f.setFolloweeId(followeeId);
        this.save(f);
        stringRedisTemplate.opsForSet().add(FOLLOWING_KEY + followerId, String.valueOf(followeeId));
        stringRedisTemplate.opsForValue().increment(FOLLOWING_COUNT_KEY + followerId);
        stringRedisTemplate.opsForValue().increment(FOLLOWER_COUNT_KEY + followeeId);
        return true;
    }

    @Override
    public long getFollowingCount(Long userId) {
        String c = stringRedisTemplate.opsForValue().get(FOLLOWING_COUNT_KEY + userId);
        if (c != null) return Long.parseLong(c);
        long count = this.count(new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));
        stringRedisTemplate.opsForValue().set(FOLLOWING_COUNT_KEY + userId, String.valueOf(count));
        return count;
    }

    @Override
    public long getFollowerCount(Long userId) {
        String c = stringRedisTemplate.opsForValue().get(FOLLOWER_COUNT_KEY + userId);
        if (c != null) return Long.parseLong(c);
        long count = this.count(new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFolloweeId, userId));
        stringRedisTemplate.opsForValue().set(FOLLOWER_COUNT_KEY + userId, String.valueOf(count));
        return count;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (ObjectUtil.hasEmpty(followerId, followeeId)) return false;
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(FOLLOWING_KEY + followerId, String.valueOf(followeeId)));
    }
}
