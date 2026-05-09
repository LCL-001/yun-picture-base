package com.lcl.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yupicture.domain.space.entity.Space;
import com.lcl.yupicture.domain.space.repository.SpaceRepository;
import com.lcl.yupicture.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}
