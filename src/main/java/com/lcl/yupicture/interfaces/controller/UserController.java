package com.lcl.yupicture.interfaces.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yupicture.infrastructure.annotation.AuthCheck;
import com.lcl.yupicture.infrastructure.common.BaseResponse;
import com.lcl.yupicture.infrastructure.common.DeleteRequest;
import com.lcl.yupicture.infrastructure.common.ResultUtils;
import com.lcl.yupicture.domain.user.constant.UserConstant;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.interfaces.assembler.UserAssembler;
import com.lcl.yupicture.interfaces.vo.user.LoginUserVO;
import com.lcl.yupicture.interfaces.vo.user.UserVO;
import com.lcl.yupicture.infrastructure.exception.BusinessException;
import com.lcl.yupicture.infrastructure.exception.ErrorCode;
import com.lcl.yupicture.infrastructure.exception.ThrowUtils;
import com.lcl.yupicture.application.service.UserApplicationService;
import com.lcl.yupicture.interfaces.dto.user.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 用户 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-04-17
 */
@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        return userApplicationService.register(userRegisterRequest);
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest userLoginRequest,
                                           HttpServletRequest request) {
        return userApplicationService.login(userLoginRequest, request);
    }

    @ApiOperation("获取当前登录用户")
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        return ResultUtils.success(BeanUtil.copyProperties(loginUser, LoginUserVO.class));
    }

    @ApiOperation("用户注销")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        return userApplicationService.logout(request);
    }

    /**
     * 创建用户
     */
    @ApiOperation("创建用户")
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User userEntity = UserAssembler.toUserEntity(userAddRequest);
        return ResultUtils.success(userApplicationService.addUser(userEntity));
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @ApiOperation("根据 id 获取用户")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        return ResultUtils.success(userApplicationService.getUserById(id));
    }

    /**
     * 根据 id 获取包装类
     */
    @ApiOperation("根据 id 获取包装类")
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        return ResultUtils.success(userApplicationService.getUserVOById(id));
    }

    /**
     * 删除用户
     */
    @ApiOperation("删除用户")
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        boolean b = userApplicationService.deleteUser(deleteRequest);
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @ApiOperation("更新用户")
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User userEntity = UserAssembler.toUserEntity(userUpdateRequest);
        userApplicationService.updateUser(userEntity);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @ApiOperation("分页获取用户封装列表")
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        return ResultUtils.success(userApplicationService.listUserVOByPage(userQueryRequest));
    }

}
