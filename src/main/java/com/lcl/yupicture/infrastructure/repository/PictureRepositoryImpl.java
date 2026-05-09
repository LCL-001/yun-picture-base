package com.lcl.yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lcl.yupicture.domain.picture.entity.Picture;
import com.lcl.yupicture.domain.picture.repository.PictureRepository;
import com.lcl.yupicture.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Service;

@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
