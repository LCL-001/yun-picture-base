package com.lcl.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yunpicturebackend.domain.dto.picture.PictureQueryRequest;
import com.lcl.yunpicturebackend.domain.dto.picture.PictureReviewRequest;
import com.lcl.yunpicturebackend.domain.dto.picture.PictureUploadByBatchRequest;
import com.lcl.yunpicturebackend.domain.dto.picture.PictureUploadRequest;
import com.lcl.yunpicturebackend.domain.po.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yunpicturebackend.domain.po.User;
import com.lcl.yunpicturebackend.domain.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 图片 服务类
 * </p>
 *
 * @author author
 * @since 2026-04-18
 */
public interface IPictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource  文件
     * @param pictureUploadRequest 上传图片请求
     * @param loginUser 登录用户
     * @return 上传图片结果
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 批量上传图片
     *
     * @param pictureUploadByBatchRequest 批量上传图片请求
     * @param loginUser                   登录用户
     * @return 批量上传图片结果
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param pictureQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片封装类
     *
     * @param picture 图片
     * @param request 请求
     * @return 图片封装类
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片分页封装类
     *
     * @param picturePage 图片分页
     * @param request 请求
     * @return 图片分页封装类
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 验证图片
     *
     * @param picture 图片
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser 登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture 图片
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);
}
