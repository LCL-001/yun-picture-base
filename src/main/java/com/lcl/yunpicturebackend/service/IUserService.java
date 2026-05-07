package com.lcl.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lcl.yunpicturebackend.common.BaseResponse;
import com.lcl.yunpicturebackend.domain.dto.user.UserLoginRequest;
import com.lcl.yunpicturebackend.domain.dto.user.UserQueryRequest;
import com.lcl.yunpicturebackend.domain.dto.user.UserRegisterRequest;
import com.lcl.yunpicturebackend.domain.po.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yunpicturebackend.domain.vo.LoginUserVO;
import com.lcl.yunpicturebackend.domain.vo.UserVO;

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
public interface IUserService extends IService<User> {

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
     * 获取加密密码
     * @param defaultPassword
     * @return
     */
    String getEncryptPassword(String defaultPassword);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

}
