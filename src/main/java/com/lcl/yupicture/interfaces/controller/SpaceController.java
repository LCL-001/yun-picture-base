package com.lcl.yupicture.interfaces.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcl.yupicture.infrastructure.annotation.AuthCheck;
import com.lcl.yupicture.infrastructure.common.BaseResponse;
import com.lcl.yupicture.infrastructure.common.DeleteRequest;
import com.lcl.yupicture.infrastructure.common.ResultUtils;
import com.lcl.yupicture.domain.user.constant.UserConstant;
import com.lcl.yupicture.domain.space.entity.Space;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.lcl.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import com.lcl.yupicture.interfaces.vo.space.SpaceVO;
import com.lcl.yupicture.infrastructure.exception.ErrorCode;
import com.lcl.yupicture.infrastructure.exception.ThrowUtils;
import com.lcl.yupicture.infrastructure.shared.auth.SpaceUserAuthManager;
import com.lcl.yupicture.application.service.SpaceApplicationService;
import com.lcl.yupicture.application.service.UserApplicationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 空间 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-04-24
 */
@RestController
@RequestMapping("/space")
@RequiredArgsConstructor
@Api(tags = "空间相关接口")
public class SpaceController {
    private final SpaceApplicationService spaceApplicationService;
    private final UserApplicationService userApplicationService;
    private final SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 创建空间
     */
    @ApiOperation("创建空间")
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        Long spaceId = spaceApplicationService.addSpace(spaceAddRequest, request);
        return ResultUtils.success(spaceId);
    }

    /**
     * 删除空间
     */
    @ApiOperation("删除空间")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        spaceApplicationService.deleteSpace(deleteRequest, request);
        return ResultUtils.success(true);
    }

    /**
     * 更新空间（仅管理员可用）
     */
    @ApiOperation("更新空间")
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                               HttpServletRequest request) {
        spaceApplicationService.updateSpace(spaceUpdateRequest, request);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @ApiOperation("根据 id 获取空间")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（封装类）
     */
    @ApiOperation("根据 id 获取空间（封装类）")
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceApplicationService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        SpaceVO spaceVO = spaceApplicationService.getSpaceVO(space, request);
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @ApiOperation("分页获取空间列表")
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(current, size),
                spaceApplicationService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间列表（封装类）
     */
    @ApiOperation("分页获取空间列表（封装类）")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                             HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Space> spacePage = spaceApplicationService.page(new Page<>(current, size),
                spaceApplicationService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return ResultUtils.success(spaceApplicationService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 编辑空间（给用户使用）
     */
    @ApiOperation("编辑空间")
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        spaceApplicationService.editSpace(spaceEditRequest, request);
        return ResultUtils.success(true);
    }

}