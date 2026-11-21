package com.example.lpsx.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.UserFeedbackService;
import com.example.lpsx.vo.UserFeedbackVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class UserFeedbackController {
    @Resource private UserFeedbackService feedbackService;

    @PostMapping
    public Result<UserFeedbackVO> create(@RequestAttribute("userId") Integer userId,
                                         @RequestBody Map<String, Object> body) {
        return Result.success(feedbackService.create(userId, body));
    }

    @GetMapping("/mine")
    public Result<Page<UserFeedbackVO>> mine(@RequestAttribute("userId") Integer userId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return Result.success(feedbackService.mine(userId, page, size));
    }
}
