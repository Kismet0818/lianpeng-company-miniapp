package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.dto.JuniorHighPrimaryDuikouDTO;
import com.example.lpsx.entity.JuniorHighPrimaryDuikou;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.mapper.JuniorHighPrimaryDuikouMapper;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import com.example.lpsx.vo.DuikouGroupPageVO;
import com.example.lpsx.vo.DuikouSchoolGroupVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/junior-high-primary-duikou")
public class JuniorHighPrimaryDuikouAdminController {
    @Resource private JuniorHighPrimaryDuikouMapper mapper;
    @Resource private JuniorHighSchoolMapper juniorHighSchoolMapper;
    @Resource private PrimarySchoolMapper primarySchoolMapper;

    @GetMapping("/junior-high-schools")
    public Result<List<JuniorHighSchool>> juniorHighSchools(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<JuniorHighSchool> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) q.like(JuniorHighSchool::getName, keyword.trim());
        q.orderByAsc(JuniorHighSchool::getName).last("LIMIT 30");
        return Result.success(juniorHighSchoolMapper.selectList(q));
    }

    @GetMapping
    public Result<DuikouGroupPageVO> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Integer admissionYear,
            @RequestParam(required = false) String juniorHighSchoolName,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String keyword) {
        page = Math.max(1, page);
        size = Math.max(1, Math.min(100, size));
        district = trim(district); juniorHighSchoolName = trim(juniorHighSchoolName); keyword = trim(keyword);
        final String keywordFilter = keyword;
        long total = mapper.countAdminGroups(admissionYear, district, juniorHighSchoolName, keyword);
        long pages = Math.max(1, (total + size - 1) / size);
        page = Math.min(page, pages);
        List<Integer> groupIds = mapper.selectAdminGroupSchoolIds(admissionYear, district, juniorHighSchoolName,
                keyword, (page - 1) * size, size);
        DuikouGroupPageVO response = new DuikouGroupPageVO();
        response.setCurrent(page); response.setSize(size); response.setTotal(total); response.setPages(pages);
        if (groupIds.isEmpty()) { response.setRecords(List.of()); return Result.success(response); }

        LambdaQueryWrapper<JuniorHighPrimaryDuikou> q = new LambdaQueryWrapper<>();
        q.in(JuniorHighPrimaryDuikou::getJuniorHighSchoolId, groupIds);
        if (admissionYear != null) q.eq(JuniorHighPrimaryDuikou::getAdmissionYear, admissionYear);
        if (!district.isBlank()) q.eq(JuniorHighPrimaryDuikou::getDistrict, district);
        if (!keywordFilter.isBlank()) q.and(x -> x.like(JuniorHighPrimaryDuikou::getPrimarySchoolName, keywordFilter)
                .or().like(JuniorHighPrimaryDuikou::getScopeText, keywordFilter));
        q.orderByDesc(JuniorHighPrimaryDuikou::getAdmissionYear)
                .orderByAsc(JuniorHighPrimaryDuikou::getDistrict)
                .orderByAsc(JuniorHighPrimaryDuikou::getJuniorHighSchoolId)
                .orderByAsc(JuniorHighPrimaryDuikou::getSortOrder)
                .orderByAsc(JuniorHighPrimaryDuikou::getId);
        List<JuniorHighPrimaryDuikou> rows = mapper.selectList(q);
        Map<Integer, JuniorHighSchool> schools = juniorHighSchoolMapper.selectBatchIds(groupIds)
                .stream().collect(Collectors.toMap(JuniorHighSchool::getId, Function.identity()));
        rows.forEach(row -> {
            JuniorHighSchool school = schools.get(row.getJuniorHighSchoolId());
            row.setJuniorHighSchoolName(school == null ? "" : school.getName());
        });
        Map<Integer, DuikouSchoolGroupVO> grouped = new LinkedHashMap<>();
        groupIds.forEach(id -> {
            DuikouSchoolGroupVO group = new DuikouSchoolGroupVO();
            JuniorHighSchool school = schools.get(id);
            group.setJuniorHighSchoolId(id);
            group.setJuniorHighSchoolName(school == null ? "" : school.getName());
            grouped.put(id, group);
        });
        rows.forEach(row -> {
            DuikouSchoolGroupVO group = grouped.get(row.getJuniorHighSchoolId());
            if (group != null) {
                if (group.getDistrict() == null) group.setDistrict(row.getDistrict());
                group.getItems().add(row);
            }
        });
        response.setRecords(List.copyOf(grouped.values()));
        return Result.success(response);
    }

    @PostMapping
    public Result<JuniorHighPrimaryDuikou> create(@RequestBody JuniorHighPrimaryDuikouDTO body) {
        JuniorHighPrimaryDuikou row = new JuniorHighPrimaryDuikou();
        apply(row, body);
        mapper.insert(row);
        return Result.success(row);
    }

    @PutMapping("/{id}")
    public Result<JuniorHighPrimaryDuikou> update(@PathVariable Integer id, @RequestBody JuniorHighPrimaryDuikouDTO body) {
        JuniorHighPrimaryDuikou row = mapper.selectById(id);
        if (row == null) throw new BusinessException("对口数据不存在");
        apply(row, body);
        mapper.updateById(row);
        return Result.success(row);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        if (mapper.selectById(id) == null) throw new BusinessException("对口数据不存在");
        mapper.deleteById(id);
        return Result.success();
    }

    private void apply(JuniorHighPrimaryDuikou row, JuniorHighPrimaryDuikouDTO body) {
        if (body == null) throw new BusinessException("请输入对口数据");
        String district = trim(body.getDistrict()), name = trim(body.getPrimarySchoolName());
        Integer juniorId = body.getJuniorHighSchoolId();
        String juniorName = trim(body.getJuniorHighSchoolName());
        if (!juniorName.isBlank()) {
            JuniorHighSchool school = juniorHighSchoolMapper.selectOne(new LambdaQueryWrapper<JuniorHighSchool>()
                    .eq(JuniorHighSchool::getName, juniorName).last("LIMIT 1"));
            if (school == null) throw new BusinessException("\u65b0\u589e\u7684\u521d\u4e2d\u5b66\u6821\u4e0d\u5b58\u5728\uff0c\u8bf7\u53bb\u5b66\u6821\u5e93\u65b0\u589e\u8fd9\u4e2a\u5b66\u6821\u540e\u518d\u6765\u6dfb\u52a0\u5bf9\u53e3\u4fe1\u606f");
            juniorId = school.getId();
        }
        if (body.getAdmissionYear() == null || juniorId == null) throw new BusinessException("招生年度和初中学校不能为空");
        if (district.isBlank() || name.isBlank()) throw new BusinessException("所属区和对口小学名称不能为空");
        if (name.length() > 150 || district.length() > 50) throw new BusinessException("名称或所属区长度超出限制");
        row.setAdmissionYear(body.getAdmissionYear()); row.setDistrict(district);
        row.setJuniorHighSchoolId(juniorId);
        var primary = primarySchoolMapper.selectOne(new LambdaQueryWrapper<com.example.lpsx.entity.PrimarySchool>()
                .eq(com.example.lpsx.entity.PrimarySchool::getName, name).last("LIMIT 1"));
        row.setPrimarySchoolId(primary == null ? null : primary.getId());
        row.setPrimarySchoolName(name); row.setRelationType(trim(body.getRelationType()).isBlank() ? "direct" : trim(body.getRelationType()));
        String admissionMethod = trim(body.getAdmissionMethod());
        row.setAdmissionMethod(admissionMethod.isBlank() ? "\u5bf9\u53e3\u5165\u5b66" : admissionMethod);
        row.setScopeText(trim(body.getScopeText())); row.setNote(trim(body.getNote()));
        if (row.getId() == null) {
            Integer maxSort = mapper.selectList(new LambdaQueryWrapper<JuniorHighPrimaryDuikou>()
                    .eq(JuniorHighPrimaryDuikou::getAdmissionYear, row.getAdmissionYear())
                    .eq(JuniorHighPrimaryDuikou::getJuniorHighSchoolId, row.getJuniorHighSchoolId())
                    .select(JuniorHighPrimaryDuikou::getSortOrder)).stream().map(JuniorHighPrimaryDuikou::getSortOrder)
                    .filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(0);
            row.setSortOrder(maxSort + 1);
        }
        row.setStatus(body.getStatus() == null ? 1 : (body.getStatus() == 0 ? 0 : 1));
        if (row.getCreatedTime() == null) row.setCreatedTime(LocalDateTime.now());
        row.setUpdatedTime(LocalDateTime.now());
    }
    private String trim(String value) { return value == null ? "" : value.trim(); }
}
