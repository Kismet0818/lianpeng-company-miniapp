package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.entity.PrimarySchool;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/appointment-schools")
public class AppointmentSchoolController {
    @Resource private PrimarySchoolMapper primaryMapper;
    @Resource private JuniorHighSchoolMapper juniorMapper;
    @Resource private ScoreQueryToolSchoolMapper highMapper;

    @GetMapping
    public Result<Map<String, List<Map<String, Object>>>> list() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("primary", primaryMapper.selectList(new LambdaQueryWrapper<PrimarySchool>()
                .eq(PrimarySchool::getTier, "一梯队")
                .orderByAsc(PrimarySchool::getDistrict).orderByAsc(PrimarySchool::getName))
                .stream().map(s -> card(s.getId(), s.getName(), s.getShortName(), s.getDistrict(),
                        s.getAddress(), s.getBackgroundImageUrl(), "primary", "小学", s.getTier())).toList());
        result.put("junior", juniorMapper.selectList(new LambdaQueryWrapper<JuniorHighSchool>()
                .eq(JuniorHighSchool::getTier, "一梯队")
                .orderByAsc(JuniorHighSchool::getDistrict).orderByAsc(JuniorHighSchool::getName))
                .stream().map(s -> card(s.getId(), s.getName(), s.getShortName(), s.getDistrict(),
                        s.getAddress(), s.getBackgroundImageUrl(), "junior", "初中", s.getTier())).toList());
        result.put("high", highMapper.selectList(new LambdaQueryWrapper<ScoreQueryToolSchool>()
                .eq(ScoreQueryToolSchool::getTier, "一梯队")
                .orderByAsc(ScoreQueryToolSchool::getDistrict).orderByAsc(ScoreQueryToolSchool::getName))
                .stream().map(s -> card(s.getId(), s.getName(), s.getShortName(), s.getDistrict(),
                        s.getAddress(), s.getLogo(), "score", "高中", s.getTier())).toList());
        return Result.success(result);
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam String source, @RequestParam Integer id) {
        Map<String, Object> item;
        if ("primary".equals(source)) {
            PrimarySchool s = primaryMapper.selectById(id);
            item = s == null ? null : detail(s.getId(), s.getName(), s.getShortName(), s.getDistrict(), s.getAddress(), s.getBackgroundImageUrl(), s.getSchoolType(), s.getTier(), s.getDescription(), s.getOfficialWebsite(), s.getSchedule(), s.getTeachingFeatures(), s.getBoardingInfo(), s.getTuitionMin(), s.getTuitionMax(), s.getCampusArea(), s.getFoundedYear(), s.getPhone(), s.getFeesStandard(), s.getFacultyStrength(), s.getFeaturesCourses(), s.getForeignSituation(), s.getCompetitionResult(), s.getEnrollmentYear(), s.getEnrollmentTarget(), s.getEnrollmentPlanning(), s.getEnrollmentWay(), s.getIsBus(), s.getLatitude(), s.getLongitude());
        } else if ("junior".equals(source)) {
            JuniorHighSchool s = juniorMapper.selectById(id);
            item = s == null ? null : detail(s.getId(), s.getName(), s.getShortName(), s.getDistrict(), s.getAddress(), s.getBackgroundImageUrl(), s.getSchoolType(), s.getTier(), s.getDescription(), s.getOfficialWebsite(), s.getSchedule(), s.getTeachingFeatures(), s.getBoardingInfo(), s.getTuitionMin(), s.getTuitionMax(), s.getCampusArea(), s.getFoundedYear(), s.getPhone(), s.getFeesStandard(), s.getFacultyStrength(), s.getFeaturesCourses(), s.getForeignSituation(), s.getCompetitionResult(), s.getEnrollmentYear(), s.getEnrollmentTarget(), s.getEnrollmentPlanning(), s.getEnrollmentWay(), s.getIsBus(), s.getLatitude(), s.getLongitude());
        } else if ("score".equals(source)) {
            ScoreQueryToolSchool s = highMapper.selectById(id);
            item = s == null ? null : detail(s.getId(), s.getName(), s.getShortName(), s.getDistrict(), s.getAddress(), s.getLogo(), s.getSchoolType(), s.getTier(), s.getDescription(), s.getOfficialWebsite(), s.getSchedule(), s.getTeachingFeatures(), s.getBoardingInfo(), s.getTuitionMin(), s.getTuitionMax(), s.getCampusArea(), s.getFoundedYear(), s.getPhone(), s.getFeesStandard(), s.getFacultyStrength(), s.getFeaturesCourses(), s.getForeignSituation(), s.getCompetitionResult(), s.getEnrollmentYear(), s.getEnrollmentTarget(), s.getEnrollmentPlanning(), s.getEnrollmentWay(), s.getIsBus(), s.getLatitude(), s.getLongitude());
        } else item = null;
        if (item == null) return Result.success(Collections.emptyMap());
        item.put("schoolSource", source);
        item.put("level", "primary".equals(source) ? "小学" : "junior".equals(source) ? "初中" : "高中");
        return Result.success(item);
    }

    private Map<String, Object> detail(Integer id, String name, String shortName, String district, String address, String image,
                                       String schoolType, String tier, String description, String officialWebsite, String schedule,
                                       String teachingFeatures, String boardingInfo, Object tuitionMin, Object tuitionMax, Object campusArea,
                                       String foundedYear, String phone, String feesStandard, String facultyStrength, String featuresCourses,
                                       String foreignSituation, String competitionResult, Integer enrollmentYear, String enrollmentTarget,
                                       String enrollmentPlanning, String enrollmentWay, String isBus, Object latitude, Object longitude) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id); item.put("name", name); item.put("shortName", shortName); item.put("district", district);
        item.put("address", address); item.put("image", image); item.put("schoolType", schoolType); item.put("tier", tier);
        item.put("description", description); item.put("officialWebsite", officialWebsite); item.put("schedule", schedule);
        item.put("teachingFeatures", teachingFeatures); item.put("boardingInfo", boardingInfo); item.put("tuitionMin", tuitionMin); item.put("tuitionMax", tuitionMax); item.put("campusArea", campusArea);
        item.put("foundedYear", foundedYear); item.put("phone", phone); item.put("feesStandard", feesStandard); item.put("facultyStrength", facultyStrength); item.put("featuresCourses", featuresCourses);
        item.put("foreignSituation", foreignSituation); item.put("competitionResult", competitionResult); item.put("enrollmentYear", enrollmentYear); item.put("enrollmentTarget", enrollmentTarget); item.put("enrollmentPlanning", enrollmentPlanning); item.put("enrollmentWay", enrollmentWay); item.put("isBus", isBus); item.put("latitude", latitude); item.put("longitude", longitude);
        return item;
    }

    private Map<String, Object> card(Integer id, String name, String shortName, String district,
                                     String address, String image, String source, String level, String tier) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id); item.put("name", name); item.put("shortName", shortName);
        item.put("district", district); item.put("address", address); item.put("image", image);
        item.put("schoolSource", source); item.put("level", level); item.put("tier", tier);
        return item;
    }
}
