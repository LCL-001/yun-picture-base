package com.lcl.yunpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lcl.yunpicturebackend.common.BaseResponse;
import com.lcl.yunpicturebackend.common.ResultUtils;
import com.lcl.yunpicturebackend.constant.UserConstant;
import com.lcl.yunpicturebackend.domain.dto.user.UserLoginRequest;
import com.lcl.yunpicturebackend.domain.dto.user.UserQueryRequest;
import com.lcl.yunpicturebackend.domain.dto.user.UserRegisterRequest;
import com.lcl.yunpicturebackend.domain.po.User;
import com.lcl.yunpicturebackend.domain.vo.LoginUserVO;
import com.lcl.yunpicturebackend.domain.vo.UserVO;
import com.lcl.yunpicturebackend.enums.UserRoleEnum;
import com.lcl.yunpicturebackend.exception.BusinessException;
import com.lcl.yunpicturebackend.exception.ErrorCode;
import com.lcl.yunpicturebackend.exception.ThrowUtils;
import com.lcl.yunpicturebackend.manager.auth.StpKit;
import com.lcl.yunpicturebackend.mapper.UserMapper;
import com.lcl.yunpicturebackend.service.IUserService;
import com.lcl.yunpicturebackend.utils.SqlSortUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.lcl.yunpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * <p>
 * 用户 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-04-17
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private PasswordEncoder passwordEncoder;


    @Override
    public BaseResponse<Long> register(UserRegisterRequest userRegisterRequest) {
        // 1. 校验
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userRegisterRequest.getUserAccount() == null || userRegisterRequest.getUserPassword() == null || userRegisterRequest.getCheckPassword() == null, ErrorCode.PARAMS_ERROR,
                "参数为空");
        ThrowUtils.throwIf(userRegisterRequest.getUserAccount().length() < 4, ErrorCode.PARAMS_ERROR,
                "用户账号过短");
        ThrowUtils.throwIf(userRegisterRequest.getUserPassword().length() < 8, ErrorCode.PARAMS_ERROR,
                "用户密码过短");
        ThrowUtils.throwIf(!userRegisterRequest.getUserPassword().equals(userRegisterRequest.getCheckPassword()), ErrorCode.PARAMS_ERROR,
                "两次输入的密码不一致");
        // 2. 校验用户是否重复
        User user = lambdaQuery().eq(User::getUserAccount, userRegisterRequest.getUserAccount()).one();
        ThrowUtils.throwIf(user != null, ErrorCode.PARAMS_ERROR, "账号重复，用户已存在");
        // 3. 密码加密
        String encryptPassword = getEncryptPassword(userRegisterRequest.getUserPassword());
        // 4. 插入数据
        User u = new User();
        u.setUserAccount(userRegisterRequest.getUserAccount());
        u.setUserPassword(encryptPassword);
        u.setUserName("默认用户名-" + UUID.randomUUID().toString().substring(0, 8));
        u.setUserAvatar("https://yuntuku-1423326981.cos.ap-guangzhou.myqcloud.com/public/2045047058943492098/2026-05-14_PELq4JjfWCnr.webp");
        u.setUserRole(UserRoleEnum.USER.getValue());
        boolean success = save(u);
        // 5. 返回结果
        ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "注册失败");
        return ResultUtils.success(u.getId());
    }

    @Override
    public BaseResponse<LoginUserVO> login(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 1. 校验
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userLoginRequest.getUserAccount() == null || userLoginRequest.getUserPassword() == null, ErrorCode.PARAMS_ERROR,
                "参数为空");
        ThrowUtils.throwIf(userLoginRequest.getUserAccount().length() < 4, ErrorCode.PARAMS_ERROR,
                "用户账号错误");
                ThrowUtils.throwIf(userLoginRequest.getUserPassword().length() < 8, ErrorCode.PARAMS_ERROR,
                "用户密码错误");
        // 2.2. 查询用户是否存在
        User user = lambdaQuery()
                .eq(User::getUserAccount, userLoginRequest.getUserAccount())
                .one();
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        // 2.3. 校验密码（BCrypt，兼容历史 MD5 并自动升级）
        ThrowUtils.throwIf(!checkAndUpgradePassword(user, userLoginRequest.getUserPassword()),
                ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        // 3. 记录用户的登录态（脱敏后会话对象，不存放密码等敏感字段）
        User sessionUser = new User();
        BeanUtil.copyProperties(user, sessionUser);
        sessionUser.setUserPassword(null);
        request.getSession().setAttribute(USER_LOGIN_STATE, sessionUser);
        // 4. 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, sessionUser);

        return ResultUtils.success(BeanUtil.copyProperties(user, LoginUserVO.class));
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        // 2. 获取当前登录的用户信息
        currentUser = getById(currentUser.getId());
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
        // 3. 注销 Sa-Token 登录态，保证登出后空间权限立即失效
        try {
            StpKit.SPACE.logout();
        } catch (Exception ignored) {
            // Sa-Token 中无对应登录态时忽略
        }
        // 4. 返回
        return ResultUtils.success(true);
    }

    public String getEncryptPassword(String userPassword) {
        return passwordEncoder.encode(userPassword);
    }

    /**
     * 校验密码：优先 BCrypt；不匹配时回退校验历史 MD5 密码，命中后自动升级为 BCrypt 存储
     *
     * @param user        用户（含数据库中的密码哈希）
     * @param rawPassword 用户输入的明文密码
     * @return 校验是否通过
     */
    private boolean checkAndUpgradePassword(User user, String rawPassword) {
        String stored = user.getUserPassword();
        if (StrUtil.isBlank(stored)) {
            return false;
        }
        if (passwordEncoder.matches(rawPassword, stored)) {
            return true;
        }
        // 兼容历史 MD5 + 固定盐的密码
        if (stored.equals(getLegacyMd5Password(rawPassword))) {
            lambdaUpdate()
                    .eq(User::getId, user.getId())
                    .set(User::getUserPassword, passwordEncoder.encode(rawPassword))
                    .update();
            return true;
        }
        return false;
    }

    /**
     * 历史 MD5 + 固定盐加密算法（仅用于登录时兼容校验，新密码一律使用 BCrypt）
     */
    private String getLegacyMd5Password(String rawPassword) {
        final String salt = "asdewqzzxcc";
        return DigestUtils.md5DigestAsHex((salt + rawPassword).getBytes());
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
        // 排序（白名单校验，防止 ORDER BY 注入）
        String safeSortField = SqlSortUtils.sanitizeSortField(sortField, "id", "userAccount", "userName",
                "userProfile", "userRole", "createTime", "editTime", "updateTime");
        queryWrapper.orderBy(StrUtil.isNotEmpty(safeSortField), "ascend".equals(sortOrder), safeSortField);
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

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }


}
