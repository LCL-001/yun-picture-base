package com.lcl.yupicture.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.infrastructure.common.BaseResponse;
import com.lcl.yupicture.interfaces.dto.user.UserLoginRequest;
import com.lcl.yupicture.interfaces.dto.user.UserQueryRequest;
import com.lcl.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.lcl.yupicture.interfaces.vo.user.LoginUserVO;
import com.lcl.yupicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 用户 服务类
 * </p>
 *
 * @author author
 * @since 2026-04-17
 */
public interface UserDomainService {

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
     * 添加用户
     *
     * @param user
     * @return
     */
    Long addUser(User user);

    /**
     * 删除用户
     *
     * @param id
     * @return
     */
    boolean deleteUser(Long id);

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
     *
     * @param defaultPassword
     * @return
     */
    String getEncryptPassword(String defaultPassword);


    /**
     * 根据id获取用户
     *
     * @param id
     * @return
     */
    User getUserById(long id);

    /**
     * 分页获取用户
     *
     * @param objectPage
     * @param queryWrapper
     * @return
     */
    Page<User> page(Page<User> objectPage, QueryWrapper<User> queryWrapper);

    /**
     * 更新用户
     *
     * @param user
     */
    void updateUser(User user);

    /**
     * 根据id列表获取用户列表
     *
     * @param userIdSet
     * @return
     */
    List<User> listByIds(Set<Long> userIdSet);
}

