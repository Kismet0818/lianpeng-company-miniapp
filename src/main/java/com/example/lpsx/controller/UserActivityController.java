package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.*;
import com.example.lpsx.mapper.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/user/activity")
@Slf4j
public class UserActivityController {
    private static final Set<String> SOURCES = Set.of("international", "score", "junior", "general", "primary", "kindergarten");
    @Resource private UserActivityMapper activityMapper;
    @Resource private SchoolMapper schoolMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreMapper;
    @Resource private JuniorHighSchoolMapper juniorMapper;
    @Resource private GeneralHighSchoolMapper generalMapper;
    @Resource private PrimarySchoolMapper primaryMapper;
    @Resource private KindergartenMapper kindergartenMapper;

    @PostMapping
    public Result<Void> record(@RequestBody Map<String, Object> body,
                               @RequestAttribute("userId") Integer userId) {
        String source = text(body, "schoolSource");
        Integer schoolId = number(body, "schoolId");
        Integer duration = number(body, "durationSeconds");
        if (!SOURCES.contains(source) || schoolId == null || schoolId <= 0) {
            // 浏览统计是后台辅助能力，参数异常不应在学校详情页返回时打断用户。
            log.warn("忽略无效学校浏览上报: userId={}, source={}, schoolId={}", userId, source, schoolId);
            return Result.success();
        }
        if (duration == null || duration < 3) return Result.success();
        duration = Math.min(duration, 7200);
        String effectiveSource = source;
        String name = resolveName(effectiveSource, schoolId);
        // 当前前端把一般高中详情沿用了 score 来源。两类高中 ID 区间不重叠：
        // score 查不到而 general 能查到时，可以无歧义地纠正来源并保留浏览数据。
        if (name == null && "score".equals(source)) {
            name = resolveName("general", schoolId);
            if (name != null) {
                effectiveSource = "general";
                log.info("纠正学校浏览来源: userId={}, source=score->general, schoolId={}", userId, schoolId);
            }
        }
        if (name == null) {
            // 前端学校来源或 ID 尚未完全统一时，宁可跳过本次统计，也不向用户弹业务错误。
            log.warn("忽略无法匹配的学校浏览上报: userId={}, source={}, schoolId={}, durationSeconds={}",
                    userId, source, schoolId, duration);
            return Result.success();
        }
        UserActivity activity = new UserActivity();
        activity.setUserId(userId); activity.setSchoolSource(effectiveSource); activity.setSchoolId(schoolId);
        activity.setSchoolName(name); activity.setEventType("school_view");
        activity.setDurationSeconds(duration); activity.setCreateTime(LocalDateTime.now());
        activityMapper.insert(activity);
        return Result.success();
    }

    /**
     * 返回当前用户最近浏览过的学校。学校身份始终由 schoolSource + schoolId 组成，
     * 避免五张学校表的相同数字主键互相串校。
     */
    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> recent(
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestAttribute("userId") Integer userId) {
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 20));
        List<Map<String, Object>> result = activityMapper.selectRecentSchools(userId, safeLimit).stream()
                .map(this::toRecentSchool)
                .filter(item -> item != null)
                .toList();
        return Result.success(result);
    }

    private Map<String, Object> toRecentSchool(Map<String, Object> activity) {
        String source = String.valueOf(activity.get("schoolSource"));
        Integer id = number(activity, "schoolId");
        if (!SOURCES.contains(source) || id == null) return null;

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("schoolId", id);
        item.put("schoolSource", source);
        item.put("lastViewTime", activity.get("lastViewTime"));
        switch (source) {
            case "international" -> {
                School school = schoolMapper.selectById(id);
                if (school == null) return null;
                item.put("name", school.getName()); item.put("schoolName", school.getName());
                item.put("district", school.getDistrict()); item.put("logoUrl", school.getLogoUrl());
                item.put("schoolType", school.getSchoolType()); item.put("grades", school.getGrades());
                item.put("curriculum", school.getCurriculum());
            }
            case "primary" -> {
                PrimarySchool school = primaryMapper.selectById(id);
                if (school == null) return null;
                item.put("name", school.getName()); item.put("schoolName", school.getName());
                item.put("district", school.getDistrict()); item.put("logoUrl", school.getBackgroundImageUrl());
                item.put("backgroundImageUrl", school.getBackgroundImageUrl());
                item.put("schoolType", school.getSchoolType()); item.put("schoolLevel", "小学");
            }
            case "kindergarten" -> {
                Kindergarten school = kindergartenMapper.selectById(id);
                if (school == null) return null;
                item.put("name", school.getName()); item.put("schoolName", school.getName());
                item.put("district", school.getDistrict()); item.put("logoUrl", school.getBackgroundImageUrl());
                item.put("backgroundImageUrl", school.getBackgroundImageUrl());
                item.put("schoolType", school.getSchoolType()); item.put("schoolLevel", "幼儿园");
            }
            case "score" -> {
                ScoreQueryToolSchool school = scoreMapper.selectById(id);
                if (school == null) return null;
                item.put("name", school.getName()); item.put("schoolName", school.getName());
                item.put("logoUrl", "score-query/school-logo/" + id + ".jpg");
                item.put("district", school.getDistrict()); item.put("schoolLevel", "高中");
            }
            case "junior" -> {
                JuniorHighSchool school = juniorMapper.selectById(id);
                if (school == null) return null;
                item.put("name", school.getName()); item.put("schoolName", school.getName());
                item.put("district", school.getDistrict()); item.put("schoolLevel", "初中");
            }
            case "general" -> {
                GeneralHighSchool school = generalMapper.selectById(id);
                if (school == null) return null;
                item.put("name", school.getName()); item.put("schoolName", school.getName());
                item.put("district", school.getDistrict()); item.put("schoolLevel", "高中");
            }
            default -> { return null; }
        }
        return item;
    }

    private String resolveName(String source, Integer id) {
        return switch (source) {
            case "international" -> { School s = schoolMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "score" -> { ScoreQueryToolSchool s = scoreMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "junior" -> { JuniorHighSchool s = juniorMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "general" -> { GeneralHighSchool s = generalMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "primary" -> { PrimarySchool s = primaryMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "kindergarten" -> { Kindergarten s = kindergartenMapper.selectById(id); yield s == null ? null : s.getName(); }
            default -> null;
        };
    }
    private String text(Map<String,Object> body, String key) { Object v=body==null?null:body.get(key); return v==null?"":String.valueOf(v).trim(); }
    private Integer number(Map<String,Object> body, String key) { try { Object v=body==null?null:body.get(key); return v==null?null:Integer.valueOf(String.valueOf(v)); } catch(Exception e){ return null; } }
}
