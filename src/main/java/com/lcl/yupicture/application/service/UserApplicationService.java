package com.lcl.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yupicture.infrastructure.common.BaseResponse;
import com.lcl.yupicture.infrastructure.common.DeleteRequest;
import com.lcl.yupicture.interfaces.dto.user.UserLoginRequest;
import com.lcl.yupicture.interfaces.dto.user.UserQueryRequest;
import com.lcl.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.lcl.yupicture.domain.user.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yupicture.interfaces.vo.user.LoginUserVO;
import com.lcl.yupicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 用户 服务类
 * </p>
 *
 * @author author
 * @since 2026-04-17
 */
public interface UserApplicationService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    BaseResponse<Long> register(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    BaseResponse<LoginUserVO> login(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    BaseResponse<Boolean> logout(HttpServletRequest request);

    /**
     * 获取查询包装类
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取用户视图
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户视图列表
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 创建用户
     * @param user
     * @return
     */
    Long addUser(User user);

    /**
     * 根据 id 获取用户
     * @param id
     * @return
     */
    User getUserById(long id);

    /**
     * 根据 id 获取包装类
     * @param id
     * @return
     */
    UserVO getUserVOById(long id);

    /**
     * 删除用户
     * @param deleteRequest
     * @return
     */
    boolean deleteUser(DeleteRequest deleteRequest);

    /**
     * 分页获取用户封装列表
     * @param userQueryRequest
     * @return
     */
    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    /**
     * 更新用户
     * @param user
     */
    void updateUser(User user);
}
