package com.lcl.yupicture.domain.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yunpicturebackend.manager.auth.StpKit;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.domain.user.repository.UserRepository;
import com.lcl.yupicture.domain.user.service.UserDomainService;
import com.lcl.yupicture.domain.user.valueobject.UserRoleEnum;
import com.lcl.yupicture.infrastructure.common.BaseResponse;
import com.lcl.yupicture.infrastructure.common.ResultUtils;
import com.lcl.yupicture.infrastructure.exception.BusinessException;
import com.lcl.yupicture.infrastructure.exception.ErrorCode;
import com.lcl.yupicture.infrastructure.exception.ThrowUtils;
import com.lcl.yupicture.interfaces.dto.user.UserLoginRequest;
import com.lcl.yupicture.interfaces.dto.user.UserQueryRequest;
import com.lcl.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.lcl.yupicture.interfaces.vo.user.LoginUserVO;
import com.lcl.yupicture.interfaces.vo.user.UserVO;
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
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserRepository userRepository;
    @Override
    public BaseResponse<Long> register(UserRegisterRequest userRegisterRequest) {
        // 2. 校验用户是否重复
        User user = userRepository.lambdaQuery()
                .eq(User::getUserAccount, userRegisterRequest.getUserAccount())
                .one();
        ThrowUtils.throwIf(user != null, ErrorCode.PARAMS_ERROR, "账号重复，用户已存在");
        // 3. 密码加密
        String encryptPassword = getEncryptPassword(userRegisterRequest.getUserPassword());
        // 4. 插入数据
        User u = new User();
        u.setUserAccount(userRegisterRequest.getUserAccount());
        u.setUserPassword(encryptPassword);
        u.setUserName("默认用户名");
        u.setUserRole(UserRoleEnum.USER.getValue());
        boolean success = userRepository.save(u);
        // 5. 返回结果
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "注册失败");
        return ResultUtils.success(u.getId());
    }

    @Override
    public BaseResponse<LoginUserVO> login(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 2. 校验用户是否存在
        // 2.1. 密码加密
        String encryptPassword = getEncryptPassword(userLoginRequest.getUserPassword());
        // 2.2. 查询用户是否存在
        User user = userRepository.lambdaQuery()
                .eq(User::getUserAccount, userLoginRequest.getUserAccount())
                .eq(User::getUserPassword, encryptPassword)
                .one();
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);

        return ResultUtils.success(BeanUtil.copyProperties(user, LoginUserVO.class));
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        // 2. 获取当前登录的用户信息
        currentUser = userRepository.getById(currentUser.getId());
        ThrowUtils.throwIf(currentUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 3. 返回
        return currentUser;
    }

    @Override
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        // 1. 判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        // 2. 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        // 3. 返回
        return ResultUtils.success(true);
    }

    public String getEncryptPassword(String userPassword) {
        final String salt = "asdewqzzxcc";
        return DigestUtils.md5DigestAsHex((salt + userPassword).getBytes());
    }

    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userRepository.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public Page<User> page(Page<User> objectPage, QueryWrapper<User> queryWrapper) {
        return userRepository.page(objectPage, queryWrapper);
    }

    @Override
    public void updateUser(User user) {
        userRepository.updateById(user);
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userRepository.listByIds(userIdSet);
    }

    @Override
    public boolean deleteUser(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public Long addUser(User user) {
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = this.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userRepository.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }
}
