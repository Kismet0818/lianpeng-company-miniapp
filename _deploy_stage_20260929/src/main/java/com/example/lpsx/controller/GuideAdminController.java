package com.example.lpsx.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.GuideArticle;
import com.example.lpsx.mapper.GuideArticleMapper;
import com.example.lpsx.service.OssService;

import jakarta.annotation.Resource;

/**
 * 择校指南管理接口（后端管理员用）
 */
@RestController
@RequestMapping("/api/admin/guide")
public class GuideAdminController {

    @Resource
    private OssService ossService;

    @Resource
    private GuideArticleMapper guideArticleMapper;

    /**
     * 上传正文插图 → OSS → 自动追加到文章末尾
     *
     * 用法: curl.exe -X POST http://192.168.1.165:8080/api/admin/guide/1/append-image -F "file=@图片.png"
     */
    @PostMapping("/{id}/append-image")
    public Result<String> appendImage(@PathVariable int id,
                                      @RequestParam("file") MultipartFile file) {
        // 上传到 OSS guide/ 目录
        String url = ossService.upload(file, "guide");

        // 查文章
        GuideArticle article = guideArticleMapper.selectById(id);
        if (article == null) {
            return Result.error("文章不存在");
        }

        // 拼 HTML 追加到 content 末尾
        String imgHtml = "<p style=\"text-align:center\"><img src=\"" + url + "\" style=\"max-width:100%\" /></p>";
        String newContent = (article.getContent() != null ? article.getContent() : "") + imgHtml;
        article.setContent(newContent);
        guideArticleMapper.updateById(article);

        return Result.success(url);
    }
}
