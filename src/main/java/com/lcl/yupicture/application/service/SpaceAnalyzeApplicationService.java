package com.lcl.yupicture.application.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.lcl.yupicture.domain.space.entity.Space;
import com.lcl.yupicture.domain.user.entity.User;
import com.lcl.yupicture.interfaces.dto.space.analyze.*;
import com.lcl.yupicture.interfaces.vo.space.analyze.*;

import java.util.List;

public interface SpaceAnalyzeApplicationService extends IService<Space> {

    /**
     * 获取空间使用情况分析
     *
     * @param spaceUsageAnalyzeRequest 空间使用情况分析请求
     * @param loginUser                登录用户
     * @return 获取空间使用情况分析结果
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 获取空间分类情况分析
     *
     * @param spaceCategoryAnalyzeRequest 空间分类情况分析请求
     * @param loginUser                   登录用户
     * @return 获取空间分类情况分析结果
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);


    /**
     * 获取空间标签情况分析
     *
     * @param spaceTagAnalyzeRequest 空间标签情况分析请求
     * @param loginUser              登录用户
     * @return 获取空间标签情况分析结果
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 获取空间大小情况分析
     *
     * @param spaceSizeAnalyzeRequest 空间大小情况分析请求
     * @param loginUser               登录用户
     * @return 获取空间大小情况分析结果
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 获取空间用户情况分析
     *
     * @param spaceUserAnalyzeRequest 空间用户情况分析请求
     * @param loginUser               登录用户
     * @return 获取空间用户情况分析结果
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间排行情况分析
     *
     * @param spaceRankAnalyzeRequest 空间排行情况分析请求
     * @param loginUser               登录用户
     * @return 获取空间排行情况分析结果
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
