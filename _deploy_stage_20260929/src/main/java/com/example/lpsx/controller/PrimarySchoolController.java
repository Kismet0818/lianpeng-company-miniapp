package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.PrimarySchool;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/primary-schools")
public class PrimarySchoolController {
    private static final int MAX_PAGE_SIZE = 500;

    @Resource
    private PrimarySchoolMapper mapper;

    @GetMapping
    public Result<Page<PrimarySchool>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String schoolType,
            @RequestParam(required = false) String tier) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        LambdaQueryWrapper<PrimarySchool> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(district)) {
            query.eq(PrimarySchool::getDistrict, district.trim());
        }
        if (StringUtils.hasText(schoolType)) {
            query.eq(PrimarySchool::getSchoolType, schoolType.trim());
        }
        if (StringUtils.hasText(tier)) {
            query.eq(PrimarySchool::getTier, tier.trim());
        }
        String search = StringUtils.hasText(keyword) ? keyword.trim()
                : (StringUtils.hasText(name) ? name.trim() : null);
        if (search != null) {
            query.and(q -> q.like(PrimarySchool::getName, search)
                    .or().like(PrimarySchool::getShortName, search));
        }
        query.orderByAsc(PrimarySchool::getDistrict)
                .orderByAsc(PrimarySchool::getName)
                .orderByAsc(PrimarySchool::getId);
        return Result.success(mapper.selectPage(new Page<>(safePage, safeSize), query));
    }

    @GetMapping("/{id}")
    public Result<PrimarySchool> detail(@PathVariable Integer id) {
        PrimarySchool school = mapper.selectById(id);
        return school == null ? Result.error("小学不存在") : Result.success(school);
    }

    @PostMapping("/compare")
    public Result<List<PrimarySchool>> compare(@RequestBody Map<String, List<Integer>> body) {
        return batchResult(body == null ? null : body.get("schoolIds"), 10, false);
    }

    @PostMapping("/batch")
    public Result<List<PrimarySchool>> batch(@RequestBody Map<String, List<Integer>> body) {
        return batchResult(body == null ? null : body.get("ids"), 20, true);
    }

    private Result<List<PrimarySchool>> batchResult(List<Integer> ids, int max, boolean emptyAllowed) {
        if (ids == null || ids.isEmpty()) {
            return emptyAllowed ? Result.success(List.of()) : Result.error("请选择学校");
        }
        if (ids.size() > max) return Result.error("单次最多" + (max == 10 ? "对比 " : "查询 ") + max + " 所学校");
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) return Result.error("学校编号不正确");
        List<Integer> uniqueIds = ids.stream().distinct().toList();
        List<PrimarySchool> schools = mapper.selectBatchIds(uniqueIds);
        if (schools.size() != uniqueIds.size()) return Result.error("学校不存在");
        return Result.success(schools);
    }
}
