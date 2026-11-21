package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.ParentQa;
import com.example.lpsx.mapper.ParentQaMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/parent-qa")
public class ParentQaController {
    @Resource private ParentQaMapper mapper;

    @GetMapping("/articles")
    public Result<Page<Map<String, Object>>> list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int size) {
        Page<ParentQa> rows = mapper.selectPage(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100)),
                new LambdaQueryWrapper<ParentQa>().eq(ParentQa::getStatus, 1).orderByDesc(ParentQa::getRanked).orderByDesc(ParentQa::getCreatedTime));
        Page<Map<String, Object>> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(rows.getRecords().stream().map(this::toList).toList());
        return Result.success(result);
    }

    @GetMapping("/articles/{id}")
    public Result<Map<String, Object>> detail(@PathVariable long id) {
        ParentQa row = mapper.selectOne(new LambdaQueryWrapper<ParentQa>().eq(ParentQa::getId, id).eq(ParentQa::getStatus, 1));
        return row == null ? Result.error(404, "问答不存在") : Result.success(toDetail(row));
    }

    private Map<String, Object> toList(ParentQa row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", row.getId()); item.put("title", row.getTitle()); item.put("summary", row.getSummary());
        item.put("coverImageUrl", row.getPicUrl()); item.put("category", row.getClassify()); item.put("sortOrder", row.getRanked()); item.put("publishedAt", row.getCreatedTime());
        return item;
    }

    private Map<String, Object> toDetail(ParentQa row) {
        Map<String, Object> item = toList(row);
        item.put("contentText", row.getContent() == null || row.getContent().isBlank() ? row.getSummary() : row.getContent());
        item.put("imageUrls", row.getPicUrl() == null || row.getPicUrl().isBlank() ? java.util.List.of() : java.util.List.of(row.getPicUrl()));
        return item;
    }
}
