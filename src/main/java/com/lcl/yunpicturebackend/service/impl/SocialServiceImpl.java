package com.lcl.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yunpicturebackend.domain.po.*;
import com.lcl.yunpicturebackend.domain.vo.PostVO;
import com.lcl.yunpicturebackend.domain.vo.UserVO;
import com.lcl.yunpicturebackend.mapper.PostMapper;
import com.lcl.yunpicturebackend.mapper.UserNotificationMapper;
import com.lcl.yunpicturebackend.service.ISocialService;
import com.lcl.yunpicturebackend.service.IUserFollowService;
import com.lcl.yunpicturebackend.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements ISocialService {

    private final StringRedisTemplate stringRedisTemplate;
    private final IUserFollowService userFollowService;
    private final PostMapper postMapper;
    private final IUserService userService;
    private final UserNotificationMapper notificationMapper;

    private static final String TIMELINE_KEY = "user:timeline:";
    private static final String UNREAD_COUNT_KEY = "user:unread:";
    private static final int TIMELINE_MAX = 200;
    private static final int FAN_THRESHOLD = 1000;

    @Override
    @Async
    public void pushToFollowersTimeline(Long userId, Long postId) {
        long fanCount = userFollowService.getFollowerCount(userId);
        if (fanCount > FAN_THRESHOLD) return;
        List<UserFollow> followers = userFollowService.list(
                new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFolloweeId, userId));
        if (CollUtil.isEmpty(followers)) return;
        long score = System.currentTimeMillis();
        for (UserFollow f : followers) {
            String key = TIMELINE_KEY + f.getFollowerId();
            stringRedisTemplate.opsForZSet().add(key, String.valueOf(postId), score);
            Long size = stringRedisTemplate.opsForZSet().size(key);
            if (size != null && size > TIMELINE_MAX) {
                stringRedisTemplate.opsForZSet().removeRange(key, 0, size - TIMELINE_MAX - 1);
            }
        }
    }

    @Override
    public Page<PostVO> getTimeline(Long userId, int current, int pageSize) {
        String key = TIMELINE_KEY + userId;
        long start = (long) (current - 1) * pageSize;
        long end = start + pageSize - 1;
        Set<String> members = stringRedisTemplate.opsForZSet().reverseRange(key, start, end);

        List<PostVO> result = new ArrayList<>();
        if (CollUtil.isNotEmpty(members)) {
            for (String m : members) {
                Long postId = Long.parseLong(m);
                Post post = postMapper.selectById(postId);
                if (post != null && post.getStatus() == 0) {
                    result.add(toPostVO(post));
                }
            }
        }

        // 数据不足，补 Pull
        if (result.size() < pageSize) {
            List<UserFollow> follows = userFollowService.list(
                    new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getFollowerId, userId));
            if (CollUtil.isNotEmpty(follows)) {
                Set<Long> existingIds = result.stream().map(PostVO::getId).collect(Collectors.toSet());
                List<Long> followeeIds = follows.stream().map(UserFollow::getFolloweeId).collect(Collectors.toList());
                List<Post> supplement = postMapper.selectList(new LambdaQueryWrapper<Post>()
                        .in(Post::getUserId, followeeIds)
                        .eq(Post::getStatus, 0)
                        .notIn(CollUtil.isNotEmpty(existingIds), Post::getId, existingIds)
                        .orderByDesc(Post::getCreateTime)
                        .last("LIMIT " + (pageSize - result.size())));
                supplement.forEach(p -> result.add(toPostVO(p)));
            }
        }

        Page<PostVO> page = new Page<>(current, pageSize);
        page.setRecords(result);
        page.setTotal(result.size());
        return page;
    }

    @Override
    public void sendNotification(Long userId, Long fromUserId, String type, Long targetId, String summary) {
        UserNotification n = new UserNotification();
        n.setUserId(userId);
        n.setFromUserId(fromUserId);
        n.setType(type);
        n.setTargetId(targetId);
        n.setSummary(summary);
        n.setIsRead(0);
        notificationMapper.insert(n);
        stringRedisTemplate.opsForValue().increment(UNREAD_COUNT_KEY + userId);
    }

    @Override
    public Page<UserNotification> listNotifications(Long userId, int current, int pageSize) {
        LambdaQueryWrapper<UserNotification> w = new LambdaQueryWrapper<>();
        w.eq(UserNotification::getUserId, userId).orderByDesc(UserNotification::getCreateTime);
        return notificationMapper.selectPage(new Page<>(current, pageSize), w);
    }

    @Override
    public long getUnreadCount(Long userId) {
        String c = stringRedisTemplate.opsForValue().get(UNREAD_COUNT_KEY + userId);
        return c != null ? Long.parseLong(c) : 0;
    }

    private PostVO toPostVO(Post post) {
        if (post == null) return null;
        PostVO vo = PostVO.objToVo(post);
        if (StrUtil.isNotBlank(post.getTags())) {
            vo.setTagList(Arrays.asList(post.getTags().split(",")));
        }
        Long uid = post.getUserId();
        if (uid != null && uid > 0) {
            User user = userService.getById(uid);
            vo.setUser(userService.getUserVO(user));
        }
        return vo;
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        UserNotification n = notificationMapper.selectById(notificationId);
        if (n != null && n.getUserId().equals(userId)) {
            notificationMapper.deleteById(notificationId);
            if (n.getIsRead() == 0) {
                String c = stringRedisTemplate.opsForValue().get(UNREAD_COUNT_KEY + userId);
                if (c != null) {
                    long v = Long.parseLong(c) - 1;
                    stringRedisTemplate.opsForValue().set(UNREAD_COUNT_KEY + userId, String.valueOf(Math.max(0, v)));
                }
            }
        }
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        UserNotification n = notificationMapper.selectById(notificationId);
        if (n != null && n.getUserId().equals(userId)) {
            n.setIsRead(1);
            notificationMapper.updateById(n);
            String c = stringRedisTemplate.opsForValue().get(UNREAD_COUNT_KEY + userId);
            if (c != null) {
                long v = Long.parseLong(c) - 1;
                stringRedisTemplate.opsForValue().set(UNREAD_COUNT_KEY + userId, String.valueOf(Math.max(0, v)));
            }
        }
    }
}
