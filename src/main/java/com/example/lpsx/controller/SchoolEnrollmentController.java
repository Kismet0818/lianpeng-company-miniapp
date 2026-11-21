package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.SchoolEnrollment;
import com.example.lpsx.mapper.SchoolEnrollmentMapper;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 民办小初招生摇号数据查询（按学校名称匹配，仅小学/初中民办）
 */
@RestController
@RequestMapping("/api/school-enrollments")
public class SchoolEnrollmentController {

    @Resource
    private SchoolEnrollmentMapper mapper;

    @GetMapping
    public Result<List<SchoolEnrollment>> list(@RequestParam(required = false) String name) {
        if (!StringUtils.hasText(name)) return Result.success(List.of());
        LambdaQueryWrapper<SchoolEnrollment> query = new LambdaQueryWrapper<>();
        query.eq(SchoolEnrollment::getSchoolName, name.trim())
                .in(SchoolEnrollment::getSchoolStage, List.of("小学", "初中"))
                .eq(SchoolEnrollment::getOwnershipType, "民办")
                .orderByAsc(SchoolEnrollment::getSchoolStage)
                .orderByDesc(SchoolEnrollment::getEnrollYear)
                .orderByAsc(SchoolEnrollment::getCampus);
        return Result.success(mapper.selectList(query));
    }
}
