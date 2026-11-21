package com.example.lpsx.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.Kindergarten;
import com.example.lpsx.mapper.KindergartenMapper;
import com.example.lpsx.vo.KindergartenVO;
import jakarta.annotation.Resource;

/** 体制内幼儿园学校库资源。 */
@RestController
@RequestMapping("/api/kindergartens")
public class KindergartenController {
    private static final int MAX_PAGE_SIZE = 500;
    @Resource private KindergartenMapper mapper;

    @GetMapping
    public Result<IPage<KindergartenVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String schoolType) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        LambdaQueryWrapper<Kindergarten> query = new LambdaQueryWrapper<Kindergarten>()
                .eq(Kindergarten::getIsActive, true);
        if (StringUtils.hasText(district)) query.eq(Kindergarten::getDistrict, district.trim());
        if (StringUtils.hasText(schoolType)) query.eq(Kindergarten::getSchoolType, schoolType.trim());
        String search = StringUtils.hasText(keyword) ? keyword.trim()
                : (StringUtils.hasText(name) ? name.trim() : null);
        if (search != null) query.and(q -> q.like(Kindergarten::getName, search)
                .or().like(Kindergarten::getShortName, search));
        query.orderByAsc(Kindergarten::getDistrict).orderByAsc(Kindergarten::getName).orderByAsc(Kindergarten::getId);
        return Result.success(mapper.selectPage(new Page<>(safePage, safeSize), query).convert(KindergartenVO::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<KindergartenVO>> detail(@PathVariable Integer id) {
        Kindergarten school = id == null || id <= 0 ? null : mapper.selectById(id);
        if (school == null || !Boolean.TRUE.equals(school.getIsActive())) {
            return ResponseEntity.status(404).body(Result.error(404, "幼儿园不存在"));
        }
        return ResponseEntity.ok(Result.success(KindergartenVO.from(school)));
    }
}
