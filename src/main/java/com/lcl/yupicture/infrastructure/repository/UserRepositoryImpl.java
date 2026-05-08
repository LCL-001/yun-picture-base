package com.lcl.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.domain.user.repository.UserRepository;
import com.lcl.yupicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
