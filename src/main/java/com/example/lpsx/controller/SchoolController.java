package com.example.lpsx.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.School;
import com.example.lpsx.entity.SchoolImage;
import com.example.lpsx.entity.PrimarySchool;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import com.example.lpsx.service.AdmissionInfoService;
import com.example.lpsx.service.SchoolImageService;
import com.example.lpsx.service.SchoolService;

import jakarta.annotation.Resource;

/**
 * 学校接口
 */
@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    @Resource
    private SchoolService schoolService;

    @Resource
    private SchoolImageService schoolImageService;

    @Resource
    private AdmissionInfoService admissionInfoService;

    @Resource
    private PrimarySchoolMapper primarySchoolMapper;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String city,
                          @RequestParam(required = false) String curriculum,
                          @RequestParam(required = false) String district,
                          @RequestParam(required = false) String grades,
                          @RequestParam(required = false) String name,
                          @RequestParam(required = false) BigDecimal tuitionMin,
                          @RequestParam(required = false) BigDecimal tuitionMax) {
        return Result.success(schoolService.listSchools(page, size, city, curriculum, district, grades, name, tuitionMin, tuitionMax));
    }
    @GetMapping("/{id}")
    public Result<School> detail(@PathVariable int id) {
        School school = id > 0 ? schoolService.getDetail(id) : null;
        return school == null ? Result.error(404, "学校不存在或已下架") : Result.success(school);
    }
    @PostMapping("/compare")
    public Result<?> compare(@RequestBody Map<String, Object> body) {
        List<Integer> ids = ids(body, "schoolIds");
        if (ids == null || ids.isEmpty()) return Result.error("请选择学校");
        if (ids.size() > 10) return Result.error("单次最多对比 10 所学校");
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) return Result.error("学校编号不正确");
        return compareBySource(ids.stream().distinct().toList(), source(body));
    }

    /** 按 ID 数组批量查询学校 — 最近浏览、对比等场景复用 */
    @PostMapping("/batch")
    public Result<?> batch(@RequestBody Map<String, Object> body) {
        List<Integer> ids = ids(body, "ids");
        if (ids == null || ids.isEmpty()) return Result.success(List.of());
        if (ids.size() > 20) return Result.error("单次最多查询 20 所学校");
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) return Result.error("学校编号不正确");
        return compareBySource(ids.stream().distinct().toList(), source(body));
    }

    /** 获取学校详情图 */
    @GetMapping("/{id}/images")
    public Result<List<SchoolImage>> images(@PathVariable int id) {
        return Result.success(schoolImageService.getBySchoolId(id));
    }

    /** 获取学校招生动态 */
    @GetMapping("/{id}/admission")
    public Result<Map<String, Object>> admission(@PathVariable int id) {
        return Result.success(admissionInfoService.getBySchoolId(id));
    }

    private Result<?> compareBySource(List<Integer> ids, String source) {
        boolean primary = "primary".equals(source)
                || ((source == null || source.isBlank()) && ids.stream().allMatch(id -> id >= 100000));
        if (primary) {
            List<PrimarySchool> schools = primarySchoolMapper.selectBatchIds(ids);
            return schools.size() == ids.size() ? Result.success(schools) : Result.error("学校不存在");
        }
        if (source != null && !source.isBlank() && !"international".equals(source)) {
            return Result.error("学校来源不正确");
        }
        List<School> schools = schoolService.compare(ids);
        return schools.size() == ids.size() ? Result.success(schools) : Result.error("学校不存在");
    }

    private String source(Map<String, Object> body) {
        Object value = body == null ? null : body.get("schoolSource");
        return value == null ? null : String.valueOf(value).trim().toLowerCase();
    }

    private List<Integer> ids(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (!(value instanceof List<?> values)) return null;
        try {
            return values.stream().map(item -> Integer.valueOf(String.valueOf(item))).toList();
        } catch (NumberFormatException exception) {
            return List.of(-1);
        }
    }
}
