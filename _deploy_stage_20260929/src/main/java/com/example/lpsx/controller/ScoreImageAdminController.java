package com.example.lpsx.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.ScoreImageBatchUploadService;

import jakarta.annotation.Resource;

/**
 * 查分工具-学校图片批量上传（临时管理端点，受 X-Admin-Key 保护）
 * <p>
 * GET /api/admin/score-image/upload?mode=all|logo|gallery
 */
@RestController
@RequestMapping("/api/admin/score-image")
public class ScoreImageAdminController {

    @Resource
    private ScoreImageBatchUploadService scoreImageBatchUploadService;

    @GetMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam(defaultValue = "all") String mode) {
        return Result.success(scoreImageBatchUploadService.upload(mode));
    }
}
