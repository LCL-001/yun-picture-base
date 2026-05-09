package com.lcl.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lcl.yupicture.interfaces.dto.space.spaceuser.SpaceUserAddRequest;
import com.lcl.yupicture.interfaces.dto.space.spaceuser.SpaceUserQueryRequest;
import com.lcl.yupicture.domain.space.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yupicture.interfaces.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 空间用户关联 服务类
 * </p>
 *
 * @author author
 * @since 2026-05-01
 */
public interface SpaceUserApplicationService extends IService<SpaceUser> {

    /**
     * 添加空间用户
     *
     * @param spaceUserAddRequest 添加请求
     * @return 添加后的空间用户id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 验证空间用户
     *
     * @param spaceUser 空间用户
     * @param add 是否是添加空间用户
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间用户视图
     *
     * @param spaceUser 空间用户
     * @param request 请求
     * @return 空间用户视图
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间用户视图列表
     *
     * @param spaceUserList 空间用户列表
     * @return 空间用户视图列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
