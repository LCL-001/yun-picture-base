package com.lcl.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lcl.yunpicturebackend.exception.BusinessException;
import com.lcl.yunpicturebackend.exception.ErrorCode;
import com.lcl.yunpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * URL 图片上传
 */
@Service
public class URLFilePictureUpload extends PictureUploadTemplate {

    /**
     * 允许下载的最大文件大小：2MB
     */
    private static final long MAX_FILE_BYTES = 2L * 1024 * 1024;

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileURL = (String) inputSource;
        // 流式下载并限制大小，防止 Content-Length 被伪造时下载超大文件
        HttpResponse response = HttpUtil.createGet(fileURL).timeout(10000).executeAsync();
        try (InputStream in = response.bodyStream();
             OutputStream out = Files.newOutputStream(file.toPath())) {
            long total = 0;
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                total += bytesRead;
                if (total > MAX_FILE_BYTES) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                }
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            response.close();
        }
    }

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

    @Override
    protected void validPicture(Object inputSource) {
        String fileURL = (String) inputSource;
        // 1. 校验非空
        ThrowUtils.throwIf(StringUtils.isBlank(fileURL), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        URL url;
        try {
            // 2. 校验 URL 格式
            url = new URL(fileURL);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式错误");
        }
        // 3. 校验 URL 协议
        ThrowUtils.throwIf(!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol()),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");
        // 4. 校验目标主机不是内网/环回地址，防止 SSRF
        validateNotInternalHost(url);
        // 5. 发送 HEAD 请求，来验证文件是否存在
        HttpResponse response = null;
        try {
            // 设置超时时间，防止长时间等待
            response = HttpUtil.createRequest(Method.HEAD, fileURL)
                    .timeout(5000)  // 5秒超时
                    .execute();

            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 6. 校验文件类型（即校验后缀）
            String contentType = response.header("Content-Type");
            if (StringUtils.isNotBlank(contentType)) {
                // 查到信息才校验，否则不校验
                // 允许的文件后缀列表
                final List<String> ALLOW_CONTENT_TYPE = Arrays.asList("image/jpg", "image/jpeg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPE.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 7. 校验文件大小（实际下载时还会做二次限制，此处仅快速失败）
            String contentLengthStr = response.header("Content-Length");
            if (StringUtils.isNotBlank(contentLengthStr)) {
                try {
                    // 查到信息才校验，否则不校验
                    long contentLength = Long.parseLong(contentLengthStr);
                    ThrowUtils.throwIf(contentLength > MAX_FILE_BYTES, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
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

    /**
     * 解析目标主机的所有 IP，拒绝环回、私网、链路本地等内网地址，防止 SSRF
     */
    private void validateNotInternalHost(URL url) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(url.getHost());
        } catch (UnknownHostException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址无法解析");
        }
        for (InetAddress address : addresses) {
            if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress() || address.isAnyLocalAddress()
                    || isUniqueLocalIpv6(address)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不允许访问内网地址");
            }
        }
    }

    /**
     * IPv6 唯一本地地址（fc00::/7）不在 isSiteLocalAddress 覆盖范围内，需单独判断
     */
    private boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] == (byte) 0xfc || bytes[0] == (byte) 0xfd);
    }
}
