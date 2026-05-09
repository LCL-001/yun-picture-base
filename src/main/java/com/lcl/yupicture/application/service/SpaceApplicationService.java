package com.lcl.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yupicture.infrastructure.common.DeleteRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import com.lcl.yupicture.domain.space.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.interfaces.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 空间 服务类
 * </p>
 *
 * @author author
 * @since 2026-04-24
 */
public interface SpaceApplicationService extends IService<Space> {

    /**
     * 添加空间
     *
     * @param spaceAddRequest 添加请求
     * @param request 请求
     * @return 添加后的空间id
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间封装类
     *
     * @param space 空间
     * @param request 请求
     * @return 空间封装类
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间分页封装类
     *
     * @param spacePage 空间分页
     * @param request 请求
     * @return 空间分页封装类
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 根据空间级别填充空间限额
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 删除空间
     *
     * @param deleteRequest 删除请求
     * @param request 请求
     */
    void deleteSpace(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest 更新请求
     * @param request 请求
     */
    void updateSpace(SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request);

    /**
     * 编辑空间
     *
     * @param spaceEditRequest 编辑请求
     * @param request 请求
     */
    void editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request);

    /**
     * 检查空间权限
     *
     * @param oldSpace 旧空间
     * @param loginUser 登录用户
     */
    void checkSpaceAuth(Space oldSpace, User loginUser);
}
