package com.lcl.yupicture.domain.user.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.io.Serializable;

import com.lcl.yupicture.domain.user.valueobject.UserRoleEnum;
import com.lcl.yupicture.infrastructure.exception.ErrorCode;
import com.lcl.yupicture.infrastructure.exception.ThrowUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author author
 * @since 2026-04-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
@ApiModel(value="User对象", description="用户")
public class User implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "账号")
    private String userAccount;

    @ApiModelProperty(value = "密码")
    private String userPassword;

    @ApiModelProperty(value = "用户昵称")
    private String userName;

    @ApiModelProperty(value = "用户头像")
    private String userAvatar;

    @ApiModelProperty(value = "用户简介")
    private String userProfile;

    @ApiModelProperty(value = "用户角色：user/admin")
    private String userRole;

    @ApiModelProperty(value = "编辑时间")
    private LocalDateTime editTime;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "是否删除")
    @TableLogic
    private Integer isDelete;

    /**
     * 校验用户注册信息
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     */
    public static void validUserRegister(String userAccount, String userPassword, String checkPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR,
                "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR,
                "用户账号过短");
        ThrowUtils.throwIf(checkPassword.length() < 8, ErrorCode.PARAMS_ERROR,
                "用户密码过短");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR,
                "两次输入的密码不一致");
    }

    /**
     * 校验用户登录信息
     *
     * @param userAccount
     * @param userPassword
     */
    public static void validUserLogin(String userAccount, String userPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR,
                "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR,
                "用户账号过短");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR,
                "用户密码过短");
    }

    /**
     * 判断是否为管理员
     *
     * @return
     */
    public boolean isAdmin() {
        return UserRoleEnum.ADMIN.getValue().equals(this.getUserRole());
    }
}
