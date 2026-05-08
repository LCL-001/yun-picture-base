package com.lcl.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lcl.yupicture.infrastructure.exception.BusinessException;
import com.lcl.yupicture.infrastructure.exception.ErrorCode;
import com.lcl.yupicture.infrastructure.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * URL 图片上传
 */
@Service
public class URLFilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileURL = (String) inputSource;
        HttpUtil.downloadFile(fileURL, file);
    }

    //    @Override
//    protected String getOriginalFilename(Object inputSource) {
//        String fileURL = (String) inputSource;
//        return FileUtil.mainName(fileURL);
//    }
// ... existing code ...
    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileURL = (String) inputSource;
        // 从 URL 中提取完整的文件名（包含所有后缀）
        try {
            URL url = new URL(fileURL);
            String path = url.getPath();
            String fileName = FileUtil.getName(path);
            // 如果文件名包含查询参数，去掉查询参数
            if (fileName != null && fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }
            return StrUtil.isNotBlank(fileName) ? fileName : "image.jpg";
        } catch (MalformedURLException e) {
            // 如果 URL 格式异常，使用原有逻辑
            return StrUtil.isNotBlank(fileURL) ? fileURL : "image.jpg";
        }
    }
// ... existing code ...


    @Override
    protected void validPicture(Object inputSource) {
        String fileURL = (String) inputSource;
        // 1. 校验非空
        ThrowUtils.throwIf(StringUtils.isBlank(fileURL), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        try {
            // 2. 校验 URL 格式
            new URL(fileURL);// 验证是否是合法的 URL
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式错误");
        }
        // 3. 校验 URL 协议
        ThrowUtils.throwIf(!fileURL.startsWith("http://") && !fileURL.startsWith("https://"),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");
        HttpResponse response = null;

        try {
            // 4. 发送 HEAD 请求，来验证文件是否存在
            // 设置超时时间，防止长时间等待
            response = HttpUtil.createRequest(Method.HEAD, fileURL)
                    .timeout(5000)  // 5秒超时
                    .execute();

            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 5. 校验文件类型（即校验后缀）
            String contentType = response.header("Content-Type");
            if (StringUtils.isNotBlank(contentType)) {
                // 查到信息才校验，否则不校验
                // 允许的文件后缀列表
                final List<String> ALLOW_CONTENT_TYPE = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPE.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 6. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StringUtils.isNotBlank(contentLengthStr)) {
                try {
                    // 查到信息才校验，否则不校验
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_M = 2 * 1024 * 1024L;  // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }

        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
