package com.example.lpsx.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.ChannelsVideo;
import com.example.lpsx.mapper.ChannelsVideoMapper;
import com.example.lpsx.service.PlatformConfigService;

import jakarta.annotation.Resource;

/**
 * 平台配置接口
 */
@RestController
@RequestMapping("/api")
public class PlatformConfigController {

    @Resource
    private PlatformConfigService configService;

    @Resource
    private ChannelsVideoMapper channelsVideoMapper;

    /** 前端获取配置（公开，无需登录） */
    @GetMapping("/config/platform")
    public Result<Map<String, String>> getConfig() {
        return Result.success(configService.getAll());
    }

    /** 获取视频号视频列表（公开） */
    @GetMapping("/config/channels-videos")
    public Result<List<ChannelsVideo>> getChannelsVideos() {
        LambdaQueryWrapper<ChannelsVideo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelsVideo::getIsActive, 1)
               .orderByAsc(ChannelsVideo::getSort);
        return Result.success(channelsVideoMapper.selectList(wrapper));
    }

    /** 后台更新配置（需 JWT 鉴权） */
    @PutMapping("/admin/config")
    public Result<Void> updateConfig(@RequestBody Map<String, String> body) {
        configService.update(body);
        return Result.success();
    }
}
