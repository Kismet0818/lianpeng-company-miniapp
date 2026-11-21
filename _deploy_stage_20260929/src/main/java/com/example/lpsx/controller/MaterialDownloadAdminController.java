package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.mapper.MaterialDownloadRecordMapper;
import com.example.lpsx.vo.MaterialDownloadRecordVO;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/material-downloads")
public class MaterialDownloadAdminController {
    @Resource private MaterialDownloadRecordMapper mapper;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "50") int size,
                                            @RequestParam(required = false) String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String value = StringUtils.hasText(keyword) ? keyword.trim() : null;
        long total = mapper.countAdmin(value);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", mapper.selectAdminPage(value, (long) (safePage - 1) * safeSize, safeSize));
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return Result.success(result);
    }
}
