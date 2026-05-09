package com.lcl.yupicture.interfaces.dto.picture;

import io.swagger.models.auth.In;
import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    /**
     * 图片搜索词
     */
    private String searchText;

    /**
     * 抓取图片数量
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;
}
