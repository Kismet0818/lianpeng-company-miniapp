package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.AdvisorService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/advisor")
public class AdvisorController {
    @Resource private AdvisorService advisorService;

    @GetMapping("/welcome")
    public Result<Map<String, Object>> welcome() {
        return Result.success(advisorService.welcome());
    }

    @PostMapping("/reply")
    public Result<Map<String, Object>> reply(@RequestBody(required = false) Map<String, Object> body) {
        String message = body == null || body.get("message") == null ? "" : String.valueOf(body.get("message"));
        Object rawContext = body == null ? null : body.get("context");
        @SuppressWarnings("unchecked")
        Map<String, Object> context = rawContext instanceof Map<?, ?> ? (Map<String, Object>) rawContext : Collections.emptyMap();
        return Result.success(advisorService.reply(message, context));
    }
}
