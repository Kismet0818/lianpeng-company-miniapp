package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.ChannelsVideo;
import com.example.lpsx.mapper.ChannelsVideoMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/channels-videos")
public class ChannelsVideoController {
    @Resource private ChannelsVideoMapper mapper;
    @GetMapping
    public Result<List<ChannelsVideo>> list() {
        return Result.success(mapper.selectList(new LambdaQueryWrapper<ChannelsVideo>()
                .eq(ChannelsVideo::getIsActive, 1).orderByAsc(ChannelsVideo::getSort).orderByAsc(ChannelsVideo::getId)));
    }
}
