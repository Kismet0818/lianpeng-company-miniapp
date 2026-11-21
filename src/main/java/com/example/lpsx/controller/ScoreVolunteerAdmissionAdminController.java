package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.dto.ScoreVolunteerAdmissionDTO;
import com.example.lpsx.entity.GeneralHighSchool;
import com.example.lpsx.entity.ScoreQueryTool;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.entity.ScoreVolunteerAdmission;
import com.example.lpsx.mapper.GeneralHighSchoolMapper;
import com.example.lpsx.mapper.ScoreQueryToolMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import com.example.lpsx.mapper.ScoreVolunteerAdmissionMapper;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 管理端：高中统一招生 1-15 志愿数据。受 /api/admin/** 管理密钥保护。 */
@RestController
@RequestMapping("/api/admin/score-volunteer-admissions")
public class ScoreVolunteerAdmissionAdminController {
    private static final String SCORE_SOURCE = "score";
    private static final String GENERAL_SOURCE = "general";

    @Resource private ScoreVolunteerAdmissionMapper admissionMapper;
    @Resource private ScoreQueryToolMapper scoreQueryToolMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreSchoolMapper;
    @Resource private GeneralHighSchoolMapper generalHighSchoolMapper;

    /**
     * 与小程序“体制内高中”一致：市重点（名额到区）、区重点、一般高中三类合并。
     */
    @GetMapping("/schools")
    public Result<Page<Map<String, Object>>> schools(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "30") long size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String district,
                                                       @RequestParam(required = false) String dataStatus) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> rows = allSchools();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            rows.removeIf(row -> !String.valueOf(row.get("schoolName")).contains(value));
        }
        if (StringUtils.hasText(district)) {
            String value = district.trim();
            rows.removeIf(row -> !value.equals(String.valueOf(row.get("district"))));
        }
        Map<String, List<Map<String, Object>>> admissions = admissionSummaryBySchool();
        if ("has".equals(dataStatus)) {
            rows.removeIf(row -> admissions.getOrDefault(key(String.valueOf(row.get("source")), (Integer) row.get("highSchoolId")), List.of()).isEmpty());
        } else if ("missing".equals(dataStatus)) {
            rows.removeIf(row -> !admissions.getOrDefault(key(String.valueOf(row.get("source")), (Integer) row.get("highSchoolId")), List.of()).isEmpty());
        }
        rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("district")))
                .thenComparing(row -> String.valueOf(row.get("schoolName"))));
        long total = rows.size();
        int from = (int) Math.min(total, (safePage - 1) * safeSize);
        int to = (int) Math.min(total, from + safeSize);
        List<Map<String, Object>> pageRows = new ArrayList<>(rows.subList(from, to));
        pageRows.forEach(row -> row.put("admissions", admissions.getOrDefault(
                key(String.valueOf(row.get("source")), (Integer) row.get("highSchoolId")), List.of())));
        Page<Map<String, Object>> result = Page.of(safePage, safeSize, total);
        result.setRecords(pageRows);
        return Result.success(result);
    }

    @GetMapping
    public Result<Page<Map<String, Object>>> list(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "50") long size,
                                                   @RequestParam(required = false) String source,
                                                   @RequestParam(required = false) Integer highSchoolId,
                                                   @RequestParam(required = false) Integer admissionYear) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<ScoreVolunteerAdmission> query = new LambdaQueryWrapper<ScoreVolunteerAdmission>()
                .eq(StringUtils.hasText(source), ScoreVolunteerAdmission::getSource, trim(source))
                .eq(highSchoolId != null, ScoreVolunteerAdmission::getHighSchoolId, highSchoolId)
                .eq(admissionYear != null, ScoreVolunteerAdmission::getAdmissionYear, admissionYear)
                .orderByDesc(ScoreVolunteerAdmission::getAdmissionYear)
                .orderByAsc(ScoreVolunteerAdmission::getTargetDistrict)
                .orderByDesc(ScoreVolunteerAdmission::getId);
        Page<ScoreVolunteerAdmission> result = admissionMapper.selectPage(Page.of(safePage, safeSize), query);
        Map<String, Map<String, Object>> schools = schoolIndex();
        Page<Map<String, Object>> view = Page.of(safePage, safeSize, result.getTotal());
        view.setRecords(result.getRecords().stream().map(row -> toView(row, schools)).toList());
        return Result.success(view);
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody ScoreVolunteerAdmissionDTO body) {
        ScoreVolunteerAdmission row = new ScoreVolunteerAdmission();
        apply(row, body);
        admissionMapper.insert(row);
        return Result.success(toView(row, schoolIndex()));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Integer id, @RequestBody ScoreVolunteerAdmissionDTO body) {
        ScoreVolunteerAdmission row = admissionMapper.selectById(id);
        if (row == null) throw new BusinessException("志愿记录不存在或已删除");
        apply(row, body);
        admissionMapper.updateById(row);
        return Result.success(toView(admissionMapper.selectById(id), schoolIndex()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        if (admissionMapper.selectById(id) == null) throw new BusinessException("志愿记录不存在或已删除");
        admissionMapper.deleteById(id);
        return Result.success();
    }

    private void apply(ScoreVolunteerAdmission row, ScoreVolunteerAdmissionDTO body) {
        if (body == null || body.getHighSchoolId() == null || body.getHighSchoolId() <= 0) throw new BusinessException("请选择高中学校");
        String source = trim(body.getSource());
        if (!SCORE_SOURCE.equals(source) && !GENERAL_SOURCE.equals(source)) throw new BusinessException("学校来源必须为 score 或 general");
        if (!StringUtils.hasText(body.getAdmissionScope())) throw new BusinessException("请填写招生范围");
        if (body.getAdmissionScope().trim().length() > 20) throw new BusinessException("招生范围不能超过 20 个字符");
        if (body.getTargetDistrict() != null && body.getTargetDistrict().trim().length() > 50) throw new BusinessException("招生区不能超过 50 个字符");
        if (body.getAdmissionYear() == null || body.getAdmissionYear() < 2000 || body.getAdmissionYear() > 2100) throw new BusinessException("招生年份不正确");
        if (body.getPlanCount() != null && body.getPlanCount() < 0) throw new BusinessException("招生人数不能小于 0");
        checkDecimal(body.getMinScore(), "最低分数");
        checkDecimal(body.getChangeRange(), "涨幅");
        if (!schoolIndex().containsKey(key(source, body.getHighSchoolId()))) throw new BusinessException("所选高中不存在或不在小程序高中学校库中");
        row.setHighSchoolId(body.getHighSchoolId());
        row.setSource(source);
        row.setAdmissionScope(body.getAdmissionScope().trim());
        row.setTargetDistrict(trimToNull(body.getTargetDistrict()));
        row.setAdmissionYear(body.getAdmissionYear());
        row.setPlanCount(body.getPlanCount());
        row.setMinScore(body.getMinScore());
        row.setChangeRange(body.getChangeRange());
    }

    private void checkDecimal(BigDecimal value, String label) {
        if (value != null && value.precision() - value.scale() > 4) throw new BusinessException(label + "数值过大");
    }

    private Map<String, Map<String, Object>> schoolIndex() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        allSchools().forEach(row -> result.put(key(String.valueOf(row.get("source")), (Integer) row.get("highSchoolId")), row));
        return result;
    }

    private Map<String, List<Map<String, Object>>> admissionSummaryBySchool() {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        admissionMapper.selectList(new LambdaQueryWrapper<ScoreVolunteerAdmission>()
                        .orderByDesc(ScoreVolunteerAdmission::getAdmissionYear)
                        .orderByAsc(ScoreVolunteerAdmission::getTargetDistrict)
                        .orderByDesc(ScoreVolunteerAdmission::getId))
                .forEach(row -> {
                    Map<String, Object> view = new HashMap<>();
                    view.put("id", row.getId()); view.put("admissionYear", row.getAdmissionYear());
                    view.put("admissionScope", row.getAdmissionScope()); view.put("targetDistrict", row.getTargetDistrict());
                    view.put("planCount", row.getPlanCount()); view.put("minScore", row.getMinScore());
                    view.put("changeRange", row.getChangeRange());
                    result.computeIfAbsent(key(row.getSource(), row.getHighSchoolId()), ignored -> new ArrayList<>()).add(view);
                });
        return result;
    }

    private List<Map<String, Object>> allSchools() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<Integer> scoreIds = new HashSet<>();
        scoreQueryToolMapper.selectList(new LambdaQueryWrapper<ScoreQueryTool>()
                        .eq(ScoreQueryTool::getTargetType, 2)
                        .eq(ScoreQueryTool::getAcademicYear, "2025-2026"))
                .forEach(row -> { if (row.getHighSchoolId() != null) scoreIds.add(row.getHighSchoolId()); });
        scoreSchoolMapper.selectList(new LambdaQueryWrapper<ScoreQueryToolSchool>()
                        .ge(ScoreQueryToolSchool::getId, 75)
                        .isNotNull(ScoreQueryToolSchool::getShortName)
                        .ne(ScoreQueryToolSchool::getShortName, ""))
                .forEach(row -> scoreIds.add(row.getId()));
        scoreSchoolMapper.selectBatchIds(scoreIds).forEach(row -> rows.add(scoreView(row)));
        generalHighSchoolMapper.selectList(new LambdaQueryWrapper<GeneralHighSchool>()).forEach(row -> rows.add(generalView(row)));
        return rows;
    }

    private Map<String, Object> scoreView(ScoreQueryToolSchool row) {
        Map<String, Object> view = new HashMap<>();
        view.put("highSchoolId", row.getId()); view.put("source", SCORE_SOURCE); view.put("schoolName", row.getName());
        view.put("district", row.getDistrict()); view.put("schoolType", row.getSchoolType()); view.put("imageUrl", row.getLogo());
        return view;
    }

    private Map<String, Object> generalView(GeneralHighSchool row) {
        Map<String, Object> view = new HashMap<>();
        view.put("highSchoolId", row.getId()); view.put("source", GENERAL_SOURCE); view.put("schoolName", row.getName());
        view.put("district", row.getDistrict()); view.put("schoolType", row.getSchoolType()); view.put("imageUrl", row.getBackgroundImageUrl());
        return view;
    }

    private Map<String, Object> toView(ScoreVolunteerAdmission row, Map<String, Map<String, Object>> schools) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", row.getId()); view.put("highSchoolId", row.getHighSchoolId()); view.put("source", row.getSource());
        view.put("admissionScope", row.getAdmissionScope()); view.put("targetDistrict", row.getTargetDistrict());
        view.put("admissionYear", row.getAdmissionYear()); view.put("planCount", row.getPlanCount());
        view.put("minScore", row.getMinScore()); view.put("changeRange", row.getChangeRange());
        view.put("createdTime", row.getCreatedTime()); view.put("updatedTime", row.getUpdatedTime());
        Map<String, Object> school = schools.get(key(row.getSource(), row.getHighSchoolId()));
        if (school != null) view.putAll(school);
        return view;
    }

    private String key(String source, Integer id) { return source + ":" + id; }
    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
