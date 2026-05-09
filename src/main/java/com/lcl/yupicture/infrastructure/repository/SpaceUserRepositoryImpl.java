package com.lcl.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yupicture.domain.space.entity.SpaceUser;
import com.lcl.yupicture.domain.space.repository.SpaceUserRepository;
import com.lcl.yupicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
