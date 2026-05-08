package com.lcl.yupicture.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yupicture.domain.user.service.UserDomainService;
import com.lcl.yupicture.infrastructure.common.BaseResponse;
import com.lcl.yupicture.infrastructure.common.DeleteRequest;
import com.lcl.yupicture.infrastructure.common.ResultUtils;
import com.lcl.yupicture.interfaces.dto.user.UserLoginRequest;
import com.lcl.yupicture.interfaces.dto.user.UserQueryRequest;
import com.lcl.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.interfaces.vo.user.LoginUserVO;
import com.lcl.yupicture.interfaces.vo.user.UserVO;
import com.lcl.yupicture.domain.user.valueobject.UserRoleEnum;
import com.lcl.yupicture.infrastructure.exception.BusinessException;
import com.lcl.yupicture.infrastructure.exception.ErrorCode;
import com.lcl.yupicture.infrastructure.exception.ThrowUtils;
import com.lcl.yunpicturebackend.manager.auth.StpKit;
import com.lcl.yupicture.infrastructure.mapper.UserMapper;
import com.lcl.yupicture.application.service.UserApplicationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.lcl.yupicture.domain.user.constant.UserConstant.USER_LOGIN_STATE;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-04-17
 */
@Service
public class UserApplicationServiceImpl extends ServiceImpl<UserMapper, User> implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;
    @Override
    public BaseResponse<Long> register(UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 1. 校验
        User.validUserRegister(userAccount, userPassword, checkPassword);
        return userDomainService.register(userRegisterRequest);
    }

    @Override
    public BaseResponse<LoginUserVO> login(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 1. 校验
        User.validUserLogin(userAccount, userPassword);
        return userDomainService.login(userLoginRequest, request);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    @Override
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return userDomainService.logout(request);
    }

    @Override
    public Long addUser(User user) {
        return userDomainService.addUser(user);
    }

    @Override
    public User getUserById(long id) {
        return userDomainService.getUserById(id);
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.deleteUser(deleteRequest.getId());
    }

    @Override
    public void updateUser(User user) {
        userDomainService.updateUser(user);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }

    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, pageSize),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userDomainService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

    public String getEncryptPassword(String userPassword) {
        return userDomainService.getEncryptPassword(userPassword);
    }
}
