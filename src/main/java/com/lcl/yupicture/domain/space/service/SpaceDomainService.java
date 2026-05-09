package com.lcl.yupicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yupicture.domain.space.entity.Space;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.infrastructure.common.DeleteRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceUpdateRequest;
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
public interface SpaceDomainService extends IService<Space> {

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间限额
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest 更新请求
     * @param request 请求
     */
    void updateSpace(SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request);

    /**
     * 检查空间权限
     *
     * @param oldSpace 旧空间
     * @param loginUser 登录用户
     */
    void checkSpaceAuth(Space oldSpace, User loginUser);
}
