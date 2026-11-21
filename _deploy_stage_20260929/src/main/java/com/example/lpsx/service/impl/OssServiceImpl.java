package com.example.lpsx.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.service.OssService;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 阿里云 OSS 服务实现
 * <p>
 * 文件命名：UUID + 原始扩展名，防止重名覆盖
 * 上传后返回完整 HTTPS URL
 */
@Slf4j
@Service
public class OssServiceImpl implements OssService {

    @Resource
    private OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    /** 直传签名有效期（秒） */
    private static final long POLICY_EXPIRE_SECONDS = 60;
    private static final Set<String> ALLOWED_POLICY_DIRS = Set.of(
            "avatar", "logo", "gallery", "qr", "guide", "banner", "lead-materials", "feedback"
    );

    @Override
    public String upload(MultipartFile file, String folder) {
        validateFile(file);

        // 生成唯一文件名：folder/uuid.ext
        String originalName = file.getOriginalFilename();
        String ext = FileUtil.extName(originalName);
        String objectName = folder + "/" + IdUtil.simpleUUID() + "." + ext;

        try (InputStream in = file.getInputStream()) {
            ossClient.putObject(bucketName, objectName, in);
            log.info("OSS 上传成功: {}", objectName);
        } catch (OSSException e) {
            log.error("OSS 上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败，请稍后重试");
        } catch (IOException e) {
            throw new BusinessException("文件读取失败");
        }

        return buildUrl(objectName);
    }

    @Override
    public String upload(MultipartFile file) {
        return upload(file, "");
    }

    @Override
    public List<String> uploadBatch(List<MultipartFile> files, String folder) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(upload(file, folder));
        }
        return urls;
    }

    @Override
    public void deleteByUrl(String url) {
        // 从完整 URL 中提取 objectName
        String prefix = "https://" + bucketName + "." + endpoint + "/";
        if (!url.startsWith(prefix)) {
            throw new BusinessException("非法的 OSS URL");
        }
        String objectName = url.substring(prefix.length());
        delete(objectName);
    }

    @Override
    public void delete(String objectName) {
        try {
            ossClient.deleteObject(bucketName, objectName);
            log.info("OSS 删除成功: {}", objectName);
        } catch (OSSException e) {
            log.error("OSS 删除失败: {}", e.getMessage(), e);
            throw new BusinessException("文件删除失败");
        }
    }

    @Override
    public Map<String, String> createUploadPolicy(String dir) {
        String cleanDir = dir == null ? "" : dir.trim().replace('\\', '/').replaceAll("^/+|/+$", "");
        if (!ALLOWED_POLICY_DIRS.contains(cleanDir)) {
            throw new BusinessException("非法的目录名");
        }
        String keyPrefix = cleanDir + "/";

        long expireEnd = System.currentTimeMillis() + POLICY_EXPIRE_SECONDS * 1000;
        Date expiration = new Date(expireEnd);

        // policy：限制文件大小 0~10MB，且 key 必须以指定目录开头
        String policyJson = "{\"expiration\":\"" + formatIso8601(expiration)
                + "\",\"conditions\":["
                + "[\"content-length-range\",0,10485760],"
                + "[\"starts-with\",\"$key\",\"" + keyPrefix + "\"]"
                + "]}";
        String policy = Base64.getEncoder()
                .encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));
        String signature = hmacSha1(policy, accessKeySecret);

        Map<String, String> result = new HashMap<>();
        result.put("host", "https://" + bucketName + "." + endpoint);
        result.put("accessid", accessKeyId);
        result.put("policy", policy);
        result.put("signature", signature);
        result.put("dir", keyPrefix);
        result.put("expire", String.valueOf(expireEnd / 1000));
        return result;
    }

    // ==================== 内部方法 ====================

    /** 格式化 ISO8601 UTC 时间（OSS policy 要求） */
    private String formatIso8601(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }

    /** HmacSHA1 签名，返回 Base64 */
    private String hmacSha1(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            log.error("OSS 签名生成失败", e);
            throw new BusinessException("生成上传签名失败");
        }
    }

    /** 允许上传的文件扩展名白名单 */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "webp", "gif", "svg", "bmp", "pdf"
    );

    /** 校验上传文件 */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        // 限制 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过 10MB");
        }
        // 文件类型白名单
        String ext = FileUtil.extName(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new BusinessException("不支持的文件类型，仅支持图片或PDF");
        }
    }

    /**
     * 从本地文件直接上传（批量工具用，保留指定文件名）
     * @param localPath  本地文件路径
     * @param objectName OSS 完整路径（如 school-gallery/171/01.png）
     * @return 完整 OSS URL
     */
    public String uploadLocalFile(String localPath, String objectName) {
        java.io.File file = new java.io.File(localPath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException("本地文件不存在: " + localPath);
        }
        try (InputStream in = new java.io.FileInputStream(file)) {
            ossClient.putObject(bucketName, objectName, in);
            log.info("OSS 上传成功: {} ← {}", objectName, localPath);
        } catch (OSSException e) {
            log.error("OSS 上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + objectName);
        } catch (IOException e) {
            throw new BusinessException("文件读取失败: " + localPath);
        }
        return buildUrl(objectName);
    }

    /** 拼接公网访问 URL */
    private String buildUrl(String objectName) {
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }
}
