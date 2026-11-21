package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.*;
import com.example.lpsx.mapper.*;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** 管理端学校库：统一管理小程序“学校库”页面使用的七类数据源。 */
@RestController
@RequestMapping("/api/admin/school-library")
public class SchoolLibraryAdminController {
    @Resource private SchoolMapper schoolMapper;
    @Resource private ScoreQueryToolMapper scoreQueryToolMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreQueryToolSchoolMapper;
    @Resource private GeneralHighSchoolMapper generalHighSchoolMapper;
    @Resource private JuniorHighSchoolMapper juniorHighSchoolMapper;
    @Resource private PrimarySchoolMapper primarySchoolMapper;
    @Resource private KindergartenMapper kindergartenMapper;
    @Resource private ObjectMapper objectMapper;

    @GetMapping("/{source}")
    public Result<Page<?>> list(@PathVariable String source,
                                @RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "20") long size,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String district,
                                @RequestParam(required = false) String schoolType,
                                @RequestParam(required = false) String level) {
        page = Math.max(1, page);
        size = Math.max(1, Math.min(100, size));
        return Result.success(switch (source) {
            case "international" -> listSchool(schoolMapper, School.class, page, size, keyword, district, schoolType, null, null);
            case "high" -> listHighSchools(page, size, keyword, district, schoolType, level);
            case "municipal" -> listMunicipal(page, size, keyword, district);
            case "key-high" -> listSchool(scoreQueryToolSchoolMapper, ScoreQueryToolSchool.class, page, size, keyword, district, schoolType, "tier", level);
            case "general-high" -> listSchool(generalHighSchoolMapper, GeneralHighSchool.class, page, size, keyword, district, schoolType, null, null);
            case "junior" -> listSchool(juniorHighSchoolMapper, JuniorHighSchool.class, page, size, keyword, district, schoolType, "tier", level);
            case "primary" -> listSchool(primarySchoolMapper, PrimarySchool.class, page, size, keyword, district, schoolType, "tier", level);
            case "kindergarten" -> listSchool(kindergartenMapper, Kindergarten.class, page, size, keyword, district, schoolType, "kindergarten_level", level);
            default -> throw new BusinessException("未知的学校数据源");
        });
    }

    /** 按小程序中的“高中”学段合并展示，页面无需理解底层表结构。 */
    private Page<Map<String, Object>> listHighSchools(long page, long size, String keyword, String district,
                                                       String schoolType, String level) {
        List<Map<String, Object>> rows = new ArrayList<>();
        scoreQueryToolSchoolMapper.selectList(schoolQuery(keyword, district, schoolType)).forEach(row -> {
            Map<String, Object> item = objectMapper.convertValue(row, Map.class);
            item.put("librarySource", "key-high");
            item.put("highSchoolType", row.getId() != null && row.getId() < 75 ? "市重点" : "区重点");
            rows.add(item);
        });
        generalHighSchoolMapper.selectList(schoolQuery(keyword, district, schoolType)).forEach(row -> {
            Map<String, Object> item = objectMapper.convertValue(row, Map.class);
            item.put("librarySource", "general-high");
            item.put("highSchoolType", "一般高中");
            rows.add(item);
        });
        if (hasText(level)) rows.removeIf(item -> !level.trim().equals(String.valueOf(item.get("highSchoolType"))));
        rows.sort(Comparator.comparing((Map<String, Object> item) -> highTypeOrder(String.valueOf(item.get("highSchoolType"))))
                .thenComparing(item -> String.valueOf(item.getOrDefault("district", "")))
                .thenComparing(item -> String.valueOf(item.getOrDefault("name", ""))));
        long total = rows.size();
        int from = (int) Math.min(total, (page - 1) * size);
        int to = (int) Math.min(total, from + size);
        Page<Map<String, Object>> result = Page.of(page, size, total);
        result.setRecords(rows.subList(from, to));
        return result;
    }

    private <T> QueryWrapper<T> schoolQuery(String keyword, String district, String schoolType) {
        return new QueryWrapper<T>().like(hasText(keyword), "name", keyword == null ? "" : keyword.trim())
                .eq(hasText(district), "district", district == null ? "" : district.trim())
                .like(hasText(schoolType), "school_type", schoolType == null ? "" : schoolType.trim());
    }

    private int highTypeOrder(String type) {
        if ("市重点".equals(type)) return 0;
        if ("区重点".equals(type)) return 1;
        return 2;
    }

    @PostMapping("/{source}")
    public Result<Object> create(@PathVariable String source, @RequestBody JsonNode body) {
        requireBody(source, body);
        Object row = convert(source, body);
        if (row instanceof School school && school.getIsActive() == null) school.setIsActive(1);
        if (row instanceof Kindergarten kindergarten && kindergarten.getIsActive() == null) kindergarten.setIsActive(true);
        mapper(source).insert(row);
        return Result.success(row);
    }

    @PutMapping("/{source}/{id}")
    public Result<Object> update(@PathVariable String source, @PathVariable Integer id, @RequestBody JsonNode body) {
        requireBody(source, body);
        BaseMapper<Object> mapper = mapper(source);
        if (mapper.selectById(id) == null) throw new BusinessException("学校数据不存在或已删除");
        Object row = convert(source, body);
        setId(row, id);
        mapper.updateById(row);
        return Result.success(mapper.selectById(id));
    }

    @DeleteMapping("/{source}/{id}")
    public Result<Void> delete(@PathVariable String source, @PathVariable Integer id) {
        BaseMapper<Object> mapper = mapper(source);
        if (mapper.selectById(id) == null) throw new BusinessException("学校数据不存在或已删除");
        mapper.deleteById(id);
        return Result.success();
    }

    private Page<?> listMunicipal(long page, long size, String keyword, String district) {
        QueryWrapper<ScoreQueryTool> query = new QueryWrapper<>();
        query.eq("target_type", 2)
                .and(hasText(keyword), q -> q.like("high_school_name", keyword.trim()).or().like("target_school_name", keyword.trim()))
                .eq(hasText(district), "high_school_district", district.trim())
                .orderByDesc("academic_year").orderByDesc("id");
        return scoreQueryToolMapper.selectPage(Page.of(page, size), query);
    }

    private <T> Page<T> listSchool(BaseMapper<T> mapper, Class<T> type, long page, long size,
                                    String keyword, String district, String schoolType,
                                    String levelColumn, String level) {
        QueryWrapper<T> query = new QueryWrapper<>();
        query.like(hasText(keyword), "name", keyword == null ? "" : keyword.trim())
                .eq(hasText(district), "district", district == null ? "" : district.trim())
                .like(hasText(schoolType), "school_type", schoolType == null ? "" : schoolType.trim())
                .eq(levelColumn != null && hasText(level), levelColumn, level == null ? "" : level.trim())
                .orderByDesc("id");
        return mapper.selectPage(Page.of(page, size), query);
    }

    @SuppressWarnings("unchecked")
    private BaseMapper<Object> mapper(String source) {
        return (BaseMapper<Object>) switch (source) {
            case "international" -> schoolMapper;
            case "municipal" -> scoreQueryToolMapper;
            case "key-high" -> scoreQueryToolSchoolMapper;
            case "general-high" -> generalHighSchoolMapper;
            case "junior" -> juniorHighSchoolMapper;
            case "primary" -> primarySchoolMapper;
            case "kindergarten" -> kindergartenMapper;
            default -> throw new BusinessException("未知的学校数据源");
        };
    }

    private Object convert(String source, JsonNode body) {
        Class<?> type = switch (source) {
            case "international" -> School.class;
            case "municipal" -> ScoreQueryTool.class;
            case "key-high" -> ScoreQueryToolSchool.class;
            case "general-high" -> GeneralHighSchool.class;
            case "junior" -> JuniorHighSchool.class;
            case "primary" -> PrimarySchool.class;
            case "kindergarten" -> Kindergarten.class;
            default -> throw new BusinessException("未知的学校数据源");
        };
        return objectMapper.convertValue(body, type);
    }

    private void requireBody(String source, JsonNode body) {
        if (body == null || !body.isObject()) throw new BusinessException("请填写学校数据");
        String field = "municipal".equals(source) ? "highSchoolName" : "name";
        if (!body.hasNonNull(field) || body.path(field).asText().isBlank()) throw new BusinessException("学校名称不能为空");
        if ("municipal".equals(source)) {
            if (!body.hasNonNull("highSchoolId")) throw new BusinessException("高中学校 ID 不能为空");
            if (!body.hasNonNull("academicYear") || body.path("academicYear").asText().isBlank()) throw new BusinessException("学年不能为空");
        }
    }

    private void setId(Object row, Integer id) {
        try { row.getClass().getMethod("setId", Integer.class).invoke(row, id); }
        catch (ReflectiveOperationException e) { throw new BusinessException("无法识别学校记录 ID"); }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
}
