package com.example.lpsx.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.AdmissionContentService;
import com.example.lpsx.vo.AdmissionContentArticleDetailVO;
import com.example.lpsx.vo.AdmissionContentArticleListVO;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
@RestController
@RequestMapping("/api/admission-content")
public class AdmissionContentController {
    private static final String MERGED_KNOWLEDGE_CATEGORY = "规划须知/志愿填报小知识";
    private static final Set<String> CATEGORIES = Set.of("详细文章", MERGED_KNOWLEDGE_CATEGORY, "志愿填报小知识", "家长问答");
    @Resource private AdmissionContentService service;
    @GetMapping("/articles")
    public Result<Page<AdmissionContentArticleListVO>> articles(@RequestParam String category, @RequestParam(required = false) Integer year, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        if (!StringUtils.hasText(category) || !CATEGORIES.contains(category)) return Result.error("文章分类不正确");
        String normalizedCategory = "志愿填报小知识".equals(category) ? MERGED_KNOWLEDGE_CATEGORY : category;
        return Result.success(service.list(normalizedCategory, year, Math.max(page, 1), Math.min(Math.max(size, 1), 100)));
    }
    @GetMapping("/articles/{id}")
    public Result<AdmissionContentArticleDetailVO> article(@PathVariable long id) {
        if (id <= 0) return Result.error(404, "文章不存在");
        AdmissionContentArticleDetailVO article = service.detail(id);
        return article == null ? Result.error(404, "文章不存在") : Result.success(article);
    }
}
