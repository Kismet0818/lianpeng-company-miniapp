package com.example.lpsx.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.ScoreVolunteer;
import com.example.lpsx.mapper.ScoreVolunteerAdmissionPublicMapper;

import jakarta.annotation.Resource;

/**
 * 查分工具-1-15志愿接口 — 公开访问（无需登录）
 * <p>
 * 1. GET /api/score-query/volunteer/meta   筛选条件（学年/目标区/学校级别）
 * 2. GET /api/score-query/volunteer        1-15志愿查询
 */
@RestController
@RequestMapping("/api/score-query/volunteer")
public class ScoreVolunteerController {

    private static final String DEFAULT_YEAR = "2025-2026";

    @Resource
    private ScoreVolunteerAdmissionPublicMapper scoreVolunteerAdmissionPublicMapper;

    /**
     * 筛选条件：学年 / 目标区 / 学校级别
     */
    @GetMapping("/meta")
    public Result<Map<String, Object>> meta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("years", scoreVolunteerAdmissionPublicMapper.selectAcademicYears());
        meta.put("districts", scoreVolunteerAdmissionPublicMapper.selectTargetDistricts());
        meta.put("schoolTypes", scoreVolunteerAdmissionPublicMapper.selectSchoolTypes());
        return Result.success(meta);
    }

    /**
     * 1-15 志愿查询
     *
     * @param district 招生目标区（考生所在区，如 徐汇区）
     * @param year     学年（如 2025-2026）
     * @param type     学校级别筛选（如 市实验性示范性高中）
     */
    @GetMapping
    public ResponseEntity<Result<List<ScoreVolunteer>>> list(@RequestParam(required = false) String district,
                                             @RequestParam(required = false) String year,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Integer highSchoolId) {
        String resolvedYear = StringUtils.hasText(year) ? year.trim() : DEFAULT_YEAR;
        if (!resolvedYear.matches("^\\d{4}-\\d{4}$")
                || !scoreVolunteerAdmissionPublicMapper.selectAcademicYears().contains(resolvedYear)) {
            return ResponseEntity.badRequest().body(Result.error(400, "年份格式错误或暂无该年份数据"));
        }
        String normalizedDistrict = StringUtils.hasText(district) ? district.trim() : null;
        String normalizedType = StringUtils.hasText(type) ? type.trim() : null;
        int admissionYear = Integer.parseInt(resolvedYear.substring(5));
        return ResponseEntity.ok(Result.success(scoreVolunteerAdmissionPublicMapper.selectVolunteerList(
                admissionYear, normalizedDistrict, normalizedType, highSchoolId)));
    }
}
