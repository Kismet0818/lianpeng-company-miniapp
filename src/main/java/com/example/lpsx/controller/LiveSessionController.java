package com.example.lpsx.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.LiveSession;
import com.example.lpsx.service.LiveSessionService;

import jakarta.annotation.Resource;

/**
 * 视频号直播接口 - 公开（无需 JWT）
 */
@RestController
@RequestMapping("/api/live")
public class LiveSessionController {

    @Resource
    private LiveSessionService liveSessionService;

    /** 直播列表 */
    @GetMapping("/list")
    public Result<List<LiveSession>> list() {
        return Result.success(liveSessionService.list());
    }
}
