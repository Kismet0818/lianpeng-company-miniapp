package com.example.lpsx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.UserFeedbackService;
import com.example.lpsx.vo.UserFeedbackVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/feedback")
public class UserFeedbackAdminController {
    @Resource private UserFeedbackService feedbackService;

    @GetMapping
    public Result<Page<UserFeedbackVO>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String keyword) {
        return Result.success(feedbackService.adminList(page, size, status, category, keyword));
    }

    @PutMapping("/{id}")
    public Result<UserFeedbackVO> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status;
        try { status = body == null || body.get("status") == null ? null : Integer.valueOf(String.valueOf(body.get("status"))); }
        catch (Exception e) { status = null; }
        String result = body == null || body.get("handlingResult") == null ? null : String.valueOf(body.get("handlingResult"));
        return Result.success(feedbackService.handle(id, status, result));
    }
}
