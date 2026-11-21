package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/junior-schools")
public class JuniorHighSchoolController {
    @Resource
    private JuniorHighSchoolMapper mapper;

    @GetMapping("/districts")
    public Result<List<String>> districts() {
        return Result.success(mapper.selectObjs(new LambdaQueryWrapper<JuniorHighSchool>()
                .select(JuniorHighSchool::getDistrict)
                .groupBy(JuniorHighSchool::getDistrict)
                .orderByAsc(JuniorHighSchool::getDistrict))
                .stream().map(Object::toString).toList());
    }

    @GetMapping
    public Result<List<JuniorHighSchool>> list(@RequestParam String district) {
        if (!StringUtils.hasText(district)) return Result.error("请选择所属区");
        LambdaQueryWrapper<JuniorHighSchool> qw = new LambdaQueryWrapper<>();
        qw.eq(JuniorHighSchool::getDistrict, district.trim())
                .orderByAsc(JuniorHighSchool::getName);
        return Result.success(mapper.selectList(qw));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Map<String, Object>>> detail(@PathVariable Integer id) {
        JuniorHighSchool school = mapper.selectById(id);
        if (school == null) {
            return ResponseEntity.status(404).body(Result.error(404, "初中学校不存在"));
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", school.getId());
        detail.put("name", school.getName());
        detail.put("shortName", school.getShortName());
        detail.put("backgroundImageUrl", school.getBackgroundImageUrl());
        detail.put("district", school.getDistrict());
        detail.put("tuitionMin", school.getTuitionMin());
        detail.put("tuitionMax", school.getTuitionMax());
        detail.put("boardingInfo", school.getBoardingInfo());
        detail.put("schoolType", school.getSchoolType());
        detail.put("tier", school.getTier());
        detail.put("address", school.getAddress());
        detail.put("description", school.getDescription());
        detail.put("officialWebsite", school.getOfficialWebsite());
        detail.put("schedule", school.getSchedule());
        detail.put("teachingFeatures", school.getTeachingFeatures());
        detail.put("campusArea", school.getCampusArea());
        detail.put("level", "junior");
        detail.put("schoolSource", "junior");
        return ResponseEntity.ok(Result.success(detail));
    }
}
