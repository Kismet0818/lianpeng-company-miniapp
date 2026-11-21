package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.dto.PlatformContentLinkDTO;
import com.example.lpsx.entity.PlatformContentLink;
import com.example.lpsx.mapper.PlatformContentLinkMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/content-links")
public class PlatformContentAdminController {
    private static final Set<String> TYPES = Set.of("OFFICIAL_ACCOUNT", "LIVE");
    @Resource private PlatformContentLinkMapper mapper;

    @GetMapping
    public Result<List<PlatformContentLink>> list() {
        return Result.success(mapper.selectList(new LambdaQueryWrapper<PlatformContentLink>()
                .orderByAsc(PlatformContentLink::getContentType)));
    }

    @PutMapping("/{type}")
    public Result<PlatformContentLink> save(@PathVariable String type,
                                            @RequestBody PlatformContentLinkDTO body) {
        String normalizedType = normalizeType(type);
        if (body == null) throw new BusinessException("请填写链接");
        String url = validHttpsUrl(body.getLinkUrl());
        String title = body.getTitle() == null ? "" : body.getTitle().trim();
        if (title.length() > 200) throw new BusinessException("标题最多200个字");

        PlatformContentLink row = mapper.selectOne(new LambdaQueryWrapper<PlatformContentLink>()
                .eq(PlatformContentLink::getContentType, normalizedType).last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            row = new PlatformContentLink();
            row.setContentType(normalizedType);
            row.setCreateTime(now);
        }
        row.setTitle(title.isBlank() ? defaultTitle(normalizedType) : title);
        row.setLinkUrl(url);
        row.setStatus(body.getStatus() == null || body.getStatus() != 0 ? 1 : 0);
        row.setUpdateTime(now);
        if (row.getId() == null) mapper.insert(row); else mapper.updateById(row);
        return Result.success(row);
    }

    @PatchMapping("/{type}/status")
    public Result<Void> status(@PathVariable String type, @RequestParam int active) {
        String normalizedType = normalizeType(type);
        PlatformContentLink row = mapper.selectOne(new LambdaQueryWrapper<PlatformContentLink>()
                .eq(PlatformContentLink::getContentType, normalizedType).last("LIMIT 1"));
        if (row == null) throw new BusinessException("该内容尚未配置");
        row.setStatus(active == 1 ? 1 : 0);
        row.setUpdateTime(LocalDateTime.now());
        mapper.updateById(row);
        return Result.success();
    }

    private String normalizeType(String type) {
        String value = type == null ? "" : type.trim().toUpperCase();
        if (!TYPES.contains(value)) throw new BusinessException("内容类型不正确");
        return value;
    }

    private String validHttpsUrl(String value) {
        try {
            String url = value == null ? "" : value.trim();
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || url.length() > 1000) {
                throw new IllegalArgumentException();
            }
            return url;
        } catch (Exception e) {
            throw new BusinessException("请填写有效的 HTTPS 链接");
        }
    }

    private String defaultTitle(String type) {
        return "LIVE".equals(type) ? "最新直播" : "最新公众号内容";
    }
}
