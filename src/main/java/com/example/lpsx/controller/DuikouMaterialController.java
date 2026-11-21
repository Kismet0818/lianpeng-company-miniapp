package com.example.lpsx.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.DuikouMaterial;
import com.example.lpsx.mapper.DuikouMaterialMapper;

import jakarta.annotation.Resource;

/**
 * 查对口：2026年16区公办学校地段对口方案（公开只读）
 */
@RestController
@RequestMapping("/api/duikou")
public class DuikouMaterialController {

    /** 上海16个行政区，用于从标题中提取所属区 */
    private static final List<String> DISTRICTS = List.of(
            "黄浦区", "徐汇区", "长宁区", "静安区", "普陀区", "虹口区", "杨浦区", "闵行区",
            "宝山区", "嘉定区", "浦东新区", "金山区", "松江区", "青浦区", "奉贤区", "崇明区");

    private static final Pattern DISTRICT_PATTERN =
            Pattern.compile("(黄浦|徐汇|长宁|静安|普陀|虹口|杨浦|闵行|宝山|嘉定|浦东新区|金山|松江|青浦|奉贤|崇明)区?");

    @Resource
    private DuikouMaterialMapper mapper;

    /**
     * 列表：不返回 content 大字段；district 非空时按所属区过滤
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String district) {
        LambdaQueryWrapper<DuikouMaterial> qw = new LambdaQueryWrapper<>();
        qw.select(DuikouMaterial::getId, DuikouMaterial::getTitle, DuikouMaterial::getSummary)
                .eq(DuikouMaterial::getStatus, 1)
                .orderByDesc(DuikouMaterial::getId);
        List<DuikouMaterial> rows = mapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DuikouMaterial row : rows) {
            String rowDistrict = extractDistrict(row.getTitle());
            if (StringUtils.hasText(district) && !district.trim().equals(rowDistrict)) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.getId());
            item.put("title", row.getTitle());
            item.put("summary", row.getSummary());
            item.put("district", rowDistrict);
            result.add(item);
        }
        return Result.success(result);
    }

    /** 各分类（区）及其条数，供前端渲染标签 */
    @GetMapping("/districts")
    public Result<List<Map<String, Object>>> districts() {
        LambdaQueryWrapper<DuikouMaterial> qw = new LambdaQueryWrapper<>();
        qw.select(DuikouMaterial::getId, DuikouMaterial::getTitle).eq(DuikouMaterial::getStatus, 1).orderByAsc(DuikouMaterial::getId);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String d : DISTRICTS) counts.put(d, 0);
        for (DuikouMaterial row : mapper.selectList(qw)) {
            String d = extractDistrict(row.getTitle());
            if (d != null) counts.merge(d, 1, Integer::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        counts.forEach((name, count) -> {
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", name);
                item.put("count", count);
                result.add(item);
            }
        });
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<DuikouMaterial> detail(@PathVariable Integer id) {
        if (id == null || id <= 0) return Result.error("参数不正确");
        DuikouMaterial material = mapper.selectOne(new LambdaQueryWrapper<DuikouMaterial>()
                .eq(DuikouMaterial::getId, id).eq(DuikouMaterial::getStatus, 1));
        return material == null ? Result.error(404, "资料不存在") : Result.success(material);
    }

    /** 从标题中提取所属区，统一为"XX区"格式；无法识别返回 null */
    private String extractDistrict(String title) {
        if (!StringUtils.hasText(title)) return null;
        Matcher m = DISTRICT_PATTERN.matcher(title);
        if (m.find()) {
            String d = m.group(1);
            return d.endsWith("区") ? d : d + "区";
        }
        return null;
    }
}
