package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.LotteryGuide;
import com.example.lpsx.mapper.LotteryGuideMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lottery-guide")
public class LotteryGuideController {
    @Resource private LotteryGuideMapper mapper;

    @GetMapping("/articles")
    public Result<Page<Map<String, Object>>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int size) {
        Page<LotteryGuide> rows = mapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100)),
                new LambdaQueryWrapper<LotteryGuide>().eq(LotteryGuide::getStatus, 1).orderByDesc(LotteryGuide::getRanked).orderByDesc(LotteryGuide::getCreatedTime));
        Page<Map<String, Object>> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(rows.getRecords().stream().map(this::toList).toList());
        return Result.success(result);
    }

    @GetMapping("/articles/{id}")
    public Result<Map<String, Object>> detail(@PathVariable long id) {
        LotteryGuide row = mapper.selectOne(new LambdaQueryWrapper<LotteryGuide>().eq(LotteryGuide::getId, id).eq(LotteryGuide::getStatus, 1));
        return row == null ? Result.error(404, "摇号指南不存在") : Result.success(toDetail(row));
    }

    private Map<String, Object> toList(LotteryGuide row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.getId()); item.put("title", row.getTitle()); item.put("summary", row.getSummary());
        item.put("coverImageUrl", row.getPicUrl()); item.put("category", row.getClassify()); item.put("sortOrder", row.getRanked()); item.put("publishedAt", row.getCreatedTime());
        item.put("status", row.getStatus());
        return item;
    }

    private Map<String, Object> toDetail(LotteryGuide row) {
        Map<String, Object> item = toList(row);
        item.put("contentText", row.getContent() == null || row.getContent().isBlank() ? row.getSummary() : row.getContent());
        item.put("imageUrls", row.getPicUrl() == null || row.getPicUrl().isBlank() ? java.util.List.of() : java.util.List.of(row.getPicUrl()));
        return item;
    }
}
