package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.PlatformContentLink;
import com.example.lpsx.mapper.PlatformContentLinkMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content-links")
public class PlatformContentController {
    @Resource private PlatformContentLinkMapper mapper;

    @GetMapping
    public Result<List<PlatformContentLink>> list(@RequestParam(required = false) String type) {
        LambdaQueryWrapper<PlatformContentLink> query = new LambdaQueryWrapper<PlatformContentLink>()
                .eq(PlatformContentLink::getStatus, 1)
                .eq(type != null && !type.isBlank(), PlatformContentLink::getContentType,
                        type == null ? null : type.trim().toUpperCase())
                .orderByAsc(PlatformContentLink::getContentType);
        return Result.success(mapper.selectList(query));
    }
}
