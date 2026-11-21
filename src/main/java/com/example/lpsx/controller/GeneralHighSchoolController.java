package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.GeneralHighSchool;
import com.example.lpsx.mapper.GeneralHighSchoolMapper;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/general-high-schools")
public class GeneralHighSchoolController {
    private static final int MAX_PAGE_SIZE = 500;

    @Resource
    private GeneralHighSchoolMapper mapper;

    @GetMapping
    public Result<Page<GeneralHighSchool>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String district) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        LambdaQueryWrapper<GeneralHighSchool> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(district)) {
            qw.eq(GeneralHighSchool::getDistrict, district.trim());
        }
        qw.orderByAsc(GeneralHighSchool::getDistrict)
                .orderByAsc(GeneralHighSchool::getName);
        return Result.success(mapper.selectPage(new Page<>(safePage, safeSize), qw));
    }

    @GetMapping("/{id}")
    public Result<GeneralHighSchool> detail(@PathVariable Integer id) {
        GeneralHighSchool school = mapper.selectById(id);
        return school == null ? Result.error("一般高中不存在") : Result.success(school);
    }
}
