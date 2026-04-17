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

    BaseResponse<Long> register(UserRegisterRequest userRegisterRequest);

    BaseResponse<LoginUserVO> login(UserLoginRequest userLoginRequest, HttpServletRequest request);

    BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request);

    BaseResponse<Boolean> logout(HttpServletRequest request);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    String getEncryptPassword(String defaultPassword);
}
