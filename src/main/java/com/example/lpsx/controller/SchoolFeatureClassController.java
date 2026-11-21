package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.SchoolFeatureClass;
import com.example.lpsx.mapper.SchoolFeatureClassMapper;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 小程序学校详情页的特色班只读接口。 */
@RestController
@RequestMapping("/api/school-feature-classes")
public class SchoolFeatureClassController {
    @Resource private SchoolFeatureClassMapper featureClassMapper;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam Integer schoolId,
                                                   @RequestParam String schoolSource) {
        if (schoolId == null || schoolId <= 0 || !StringUtils.hasText(schoolSource)) return Result.success(List.of());
        String source = schoolSource.trim();
        if (!List.of("junior", "score", "general").contains(source)) return Result.success(List.of());
        List<Map<String, Object>> rows = featureClassMapper.selectList(new LambdaQueryWrapper<SchoolFeatureClass>()
                        .eq(SchoolFeatureClass::getSchoolId, schoolId)
                        .orderByAsc(SchoolFeatureClass::getFeatureClass)
                        .orderByAsc(SchoolFeatureClass::getId))
                .stream()
                .filter(row -> source.equals(normalizeSource(row.getSource())))
                .map(row -> Map.<String, Object>of(
                        "id", row.getId(),
                        "featureClass", row.getFeatureClass(),
                        "features", value(row.getFeatures()),
                        "classCount", value(row.getClassCount()),
                        "classAssignment", value(row.getClassAssignment())))
                .toList();
        return Result.success(rows);
    }

    private String normalizeSource(String value) {
        String source = value == null ? "" : value.trim();
        if (source.startsWith("junior_high_school")) return "junior";
        if (source.startsWith("score_query_tool_school")) return "score";
        if (source.startsWith("general_high_school")) return "general";
        return source;
    }

    private String value(String value) { return value == null ? "" : value; }
}
