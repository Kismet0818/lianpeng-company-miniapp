package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.dto.SchoolFeatureClassDTO;
import com.example.lpsx.entity.GeneralHighSchool;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.entity.SchoolFeatureClass;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.mapper.GeneralHighSchoolMapper;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.SchoolFeatureClassMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 管理端学校特色班 CRUD；学校列表始终以学校库为准，不依赖特色班表是否已有数据。 */
@RestController
@RequestMapping("/api/admin/school-feature-classes")
public class SchoolFeatureClassAdminController {
    private static final String JUNIOR_SOURCE = "junior";
    private static final String GENERAL_SOURCE = "general";
    private static final String SCORE_SOURCE = "score";

    @Resource private SchoolFeatureClassMapper featureClassMapper;
    @Resource private JuniorHighSchoolMapper juniorHighSchoolMapper;
    @Resource private GeneralHighSchoolMapper generalHighSchoolMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreQueryToolSchoolMapper;

    @GetMapping("/schools")
    public Result<Page<Map<String, Object>>> schools(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String district,
                                                      @RequestParam(required = false) String stage,
                                                      @RequestParam(required = false) String dataStatus) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 100);
        List<Map<String, Object>> rows = allSchools(stage);
        if (StringUtils.hasText(keyword)) rows.removeIf(row -> !String.valueOf(row.get("schoolName")).contains(keyword.trim()));
        if (StringUtils.hasText(district)) rows.removeIf(row -> !district.trim().equals(row.get("district")));

        Map<String, List<Map<String, Object>>> records = recordsBySchool();
        if ("has".equals(dataStatus)) rows.removeIf(row -> records.getOrDefault(key(row), List.of()).isEmpty());
        if ("missing".equals(dataStatus)) rows.removeIf(row -> !records.getOrDefault(key(row), List.of()).isEmpty());
        rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("district")))
                .thenComparing(row -> String.valueOf(row.get("schoolName"))));

        long total = rows.size();
        int from = (int) Math.min(total, (safePage - 1) * safeSize);
        int to = (int) Math.min(total, from + safeSize);
        List<Map<String, Object>> pageRows = new ArrayList<>(rows.subList(from, to));
        pageRows.forEach(row -> row.put("featureClasses", records.getOrDefault(key(row), List.of())));
        Page<Map<String, Object>> result = Page.of(safePage, safeSize, total);
        result.setRecords(pageRows);
        return Result.success(result);
    }

    @PostMapping
    public Result<SchoolFeatureClass> create(@RequestBody SchoolFeatureClassDTO body) {
        SchoolFeatureClass row = new SchoolFeatureClass();
        apply(row, body);
        featureClassMapper.insert(row);
        return Result.success(featureClassMapper.selectById(row.getId()));
    }

    @PutMapping("/{id}")
    public Result<SchoolFeatureClass> update(@PathVariable Integer id, @RequestBody SchoolFeatureClassDTO body) {
        SchoolFeatureClass row = featureClassMapper.selectById(id);
        if (row == null) throw new BusinessException("特色班记录不存在或已删除");
        apply(row, body);
        featureClassMapper.updateById(row);
        return Result.success(featureClassMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        if (featureClassMapper.selectById(id) == null) throw new BusinessException("特色班记录不存在或已删除");
        featureClassMapper.deleteById(id);
        return Result.success();
    }

    private void apply(SchoolFeatureClass row, SchoolFeatureClassDTO body) {
        if (body == null || body.getSchoolId() == null || body.getSchoolId() <= 0) throw new BusinessException("请选择学校");
        String source = trim(body.getSource());
        if (!JUNIOR_SOURCE.equals(source) && !GENERAL_SOURCE.equals(source) && !SCORE_SOURCE.equals(source)) throw new BusinessException("学校来源必须为 junior、general 或 score");
        if (!StringUtils.hasText(body.getFeatureClass())) throw new BusinessException("请填写特色班型");
        if (body.getFeatureClass().trim().length() > 255) throw new BusinessException("特色班型不能超过 255 个字符");
        Map<String, Object> school = schoolIndex().get(key(source, body.getSchoolId()));
        if (school == null) throw new BusinessException("所选学校不存在或不在当前学校库中");
        row.setSchoolId(body.getSchoolId());
        row.setSource(source);
        row.setDistrict(String.valueOf(school.get("district")));
        row.setFeatureClass(body.getFeatureClass().trim());
        row.setFeatures(trimToNull(body.getFeatures()));
        row.setClassCount(trimToNull(body.getClassCount()));
        row.setClassAssignment(trimToNull(body.getClassAssignment()));
    }

    private Map<String, List<Map<String, Object>>> recordsBySchool() {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        featureClassMapper.selectList(new LambdaQueryWrapper<SchoolFeatureClass>()
                        .orderByAsc(SchoolFeatureClass::getFeatureClass).orderByDesc(SchoolFeatureClass::getId))
                .forEach(row -> result.computeIfAbsent(key(normalizeSource(row.getSource()), row.getSchoolId()), ignored -> new ArrayList<>()).add(toView(row)));
        return result;
    }

    private Map<String, Map<String, Object>> schoolIndex() {
        Map<String, Map<String, Object>> index = new HashMap<>();
        allSchools(null).forEach(row -> index.put(key(row), row));
        return index;
    }

    private List<Map<String, Object>> allSchools(String stage) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (!StringUtils.hasText(stage) || "junior".equals(stage)) juniorHighSchoolMapper.selectList(null).forEach(row -> rows.add(juniorView(row)));
        if (!StringUtils.hasText(stage) || "general".equals(stage)) {
            scoreQueryToolSchoolMapper.selectList(null).forEach(row -> rows.add(scoreView(row)));
            generalHighSchoolMapper.selectList(null).forEach(row -> rows.add(generalView(row)));
        }
        return rows;
    }

    private Map<String, Object> juniorView(JuniorHighSchool row) { return schoolView(row.getId(), JUNIOR_SOURCE, "初中", row.getName(), row.getDistrict(), row.getSchoolType(), row.getBackgroundImageUrl()); }
    private Map<String, Object> scoreView(ScoreQueryToolSchool row) { return schoolView(row.getId(), SCORE_SOURCE, "高中", row.getName(), row.getDistrict(), row.getSchoolType(), row.getLogo()); }
    private Map<String, Object> generalView(GeneralHighSchool row) { return schoolView(row.getId(), GENERAL_SOURCE, "高中", row.getName(), row.getDistrict(), row.getSchoolType(), row.getBackgroundImageUrl()); }
    private Map<String, Object> schoolView(Integer id, String source, String stage, String name, String district, String type, String imageUrl) {
        Map<String, Object> view = new HashMap<>();
        view.put("schoolId", id); view.put("source", source); view.put("stage", stage); view.put("schoolName", name);
        view.put("district", district); view.put("schoolType", type); view.put("imageUrl", imageUrl); return view;
    }
    private Map<String, Object> toView(SchoolFeatureClass row) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", row.getId()); view.put("schoolId", row.getSchoolId()); view.put("source", normalizeSource(row.getSource()));
        view.put("district", row.getDistrict()); view.put("featureClass", row.getFeatureClass()); view.put("features", row.getFeatures());
        view.put("classCount", row.getClassCount()); view.put("classAssignment", row.getClassAssignment());
        view.put("createdTime", row.getCreatedTime()); view.put("updatedTime", row.getUpdatedTime()); return view;
    }
    private String key(Map<String, Object> row) { return key(String.valueOf(row.get("source")), (Integer) row.get("schoolId")); }
    private String key(String source, Integer schoolId) { return source + ":" + schoolId; }
    /** 兼容 Excel 历史导入的来源文字，统一到管理端使用的学校库标识。 */
    private String normalizeSource(String source) {
        String value = trim(source);
        if (value.startsWith("junior_high_school")) return JUNIOR_SOURCE;
        if (value.startsWith("score_query_tool_school")) return SCORE_SOURCE;
        if (value.startsWith("general_high_school")) return GENERAL_SOURCE;
        return value;
    }
    private String trim(String value) { return value == null ? "" : value.trim(); }
    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
