package com.lcl.yunpicturebackend.service.impl;

import com.lcl.yunpicturebackend.domain.po.User;
import com.lcl.yunpicturebackend.mapper.UserMapper;
import com.lcl.yunpicturebackend.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-04-17
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
