package com.example.lpsx.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.OssService;

import jakarta.annotation.Resource;

/**
 * OSS 文件上传接口
 * <p>
 * 开发阶段不做 JWT 认证，WebMvcConfig 中已放行 /api/oss/**
 */
@RestController
@RequestMapping("/api/oss")
public class OssController {

    private static final int TEMP_ICON_MAX_FILES = 20;
    private static final long TEMP_ICON_MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final long TEMP_ICON_MAX_BATCH_SIZE = 30L * 1024 * 1024;
    private static final Set<String> TEMP_ICON_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final String TEMP_ICON_FOLDER = "temp-miniapp-icons";

    @Resource
    private OssService ossService;

    /**
     * 获取前端直传 OSS 的签名（小程序端使用，需登录）
     * <p>
     * 前端拿到 host/policy/signature 后，用 wx.uploadFile 直接上传到 OSS，
     * 不经过后端，保证 HTTPS 且不暴露 AccessKey Secret。
     *
     * @param body {"dir": "avatar"}，dir 可选，限制上传目录
     */
    @PostMapping("/policy")
    public Result<Map<String, String>> policy(@RequestBody(required = false) Map<String, String> body) {
        String dir = (body == null) ? "" : body.getOrDefault("dir", "");
        return Result.success(ossService.createUploadPolicy(dir));
    }

    /**
     * 单文件上传
     *
     * @param file   文件
     * @param folder 可选目录（默认空=根目录）
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false, defaultValue = "") String folder) {
        String url = ossService.upload(file, folder);
        return Result.success(url);
    }

    /**
     * 批量上传（最多 9 张）
     *
     * @param files  文件列表
     * @param folder 可选目录
     */
    @PostMapping("/upload-batch")
    public Result<List<String>> uploadBatch(@RequestParam("files") List<MultipartFile> files,
                                            @RequestParam(required = false, defaultValue = "") String folder) {
        if (files.size() > 9) {
            return Result.error("单次最多上传 9 个文件");
        }
        List<String> urls = ossService.uploadBatch(files, folder);
        return Result.success(urls);
    }

    /**
     * 小程序联调期间使用的临时图标批量上传接口。
     *
     * <p>该接口不校验 JWT 和管理密钥，存储目录固定，避免调用方写入任意 OSS 目录。
     * 仅允许常见位图，最多 20 个、单文件 5MB、单批次 30MB。上线前应删除本接口及
     * WebMvcConfig 中对应的放行配置。</p>
     */
    @PostMapping("/temp/icon-batch")
    public Result<List<String>> uploadTemporaryIcons(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Result.error("请选择需要上传的图标文件");
        }
        if (files.size() > TEMP_ICON_MAX_FILES) {
            return Result.error("单次最多上传 " + TEMP_ICON_MAX_FILES + " 个文件");
        }

        long totalSize = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }
            if (file.getSize() > TEMP_ICON_MAX_FILE_SIZE) {
                return Result.error("单个图标不能超过 5MB");
            }
            totalSize += file.getSize();
            if (totalSize > TEMP_ICON_MAX_BATCH_SIZE) {
                return Result.error("单批文件总大小不能超过 30MB");
            }

            String originalName = file.getOriginalFilename();
            int dot = originalName == null ? -1 : originalName.lastIndexOf('.');
            String extension = dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase();
            String contentType = file.getContentType();
            if (!TEMP_ICON_EXTENSIONS.contains(extension)
                    || contentType == null
                    || !contentType.toLowerCase().startsWith("image/")) {
                return Result.error("仅支持 jpg、jpeg、png、webp、gif 图标文件");
            }
        }

        return Result.success(ossService.uploadBatch(files, TEMP_ICON_FOLDER));
    }

    /**
     * 根据 URL 删除文件
     */
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return Result.error("url 不能为空");
        }
        ossService.deleteByUrl(url);
        return Result.success();
    }
}
