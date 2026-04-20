package com.lcl.yunpicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.lcl.yunpicturebackend.config.CosClientConfig;
import com.lcl.yunpicturebackend.domain.dto.file.UploadPictureResult;
import com.lcl.yunpicturebackend.exception.BusinessException;
import com.lcl.yunpicturebackend.exception.ErrorCode;
import com.lcl.yunpicturebackend.manager.CosManager;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    private static final List<String> IMAGE_FORMATS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif", "bmp");

    // ... existing code ...

    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);
        // 2. 获取图片上传地址
        String uuid = RandomUtil.randomString(12);
        String originalFilename = getOriginalFilename(inputSource);
        // 获取文件扩展名（智能识别）
        String suffix = extractImageSuffix(originalFilename, inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 4. 处理文件来源(本地或 URL)
            processFile(inputSource, file);
            // 5. 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 6. 获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            return buildResult(originalFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }

    }

    /**
     * 智能提取图片后缀
     * @param filename 文件名
     * @param inputSource 输入源（用于从 URL 或其他来源获取更多信息）
     * @return 图片后缀
     */
    private String extractImageSuffix(String filename, Object inputSource) {
        if (StrUtil.isBlank(filename)) {
            return "jpg";
        }

        String lowerFilename = filename.toLowerCase();

        // 策略1：遍历常见图片格式，查找第一个匹配的
        for (String format : IMAGE_FORMATS) {
            int index = lowerFilename.indexOf("." + format);
            if (index != -1) {
                // 检查是否是真正的扩展名（后面跟着字符串结束或非字母数字字符）
                int afterDotIndex = index + format.length() + 1;
                if (afterDotIndex >= filename.length() ||
                        !Character.isLetterOrDigit(filename.charAt(afterDotIndex))) {
                    return format;
                }
            }
        }

        // 策略2：如果是 URL，尝试从 Content-Type 推断（仅对 URL 有效）
        if (inputSource instanceof String && (((String) inputSource).startsWith("http") ||
                ((String) inputSource).startsWith("https"))) {
            String inferredSuffix = inferSuffixFromUrl((String) inputSource);
            if (StrUtil.isNotBlank(inferredSuffix)) {
                return inferredSuffix;
            }
        }

        // 策略3：使用传统方法获取最后一个后缀
        String suffix = FileUtil.getSuffix(filename);
        if (StrUtil.isNotBlank(suffix) && IMAGE_FORMATS.contains(suffix.toLowerCase())) {
            return suffix.toLowerCase();
        }

        // 默认返回 jpg
        return "jpg";
    }

    /**
     * 从 URL 推断图片后缀（通过 HEAD 请求获取 Content-Type）
     * @param url URL 地址
     * @return 图片后缀
     */
    private String inferSuffixFromUrl(String url) {
        try {
            HttpResponse response = HttpUtil.createRequest(Method.HEAD, url)
                    .timeout(3000)
                    .execute();

            if (response.getStatus() == HttpStatus.HTTP_OK) {
                String contentType = response.header("Content-Type");
                if (StrUtil.isNotBlank(contentType)) {
                    if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                        return "jpg";
                    } else if (contentType.contains("png")) {
                        return "png";
                    } else if (contentType.contains("webp")) {
                        return "webp";
                    } else if (contentType.contains("gif")) {
                        return "gif";
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从 URL 推断后缀失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 构建上传结果
     * @param originalFilename
     * @param file
     * @param uploadPath
     * @param imageInfo
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, File file, String uploadPath, ImageInfo imageInfo) {
        // 封装返回结果
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }

    /**
     * 处理输入源(本地或 URL)，并生成临时文件
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 获取输入源的原始文件名
     * @param inputSource
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验输入源(本地或 URL)
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 删除临时文件
     * @param file
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean delete = file.delete();
        if (!delete) {
            log.error("删除临时文件失败, 文件路径={}", file.getAbsolutePath());
        }
    }


}
