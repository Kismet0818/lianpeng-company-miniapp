package com.example.lpsx.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.AdmissionInfo;
import com.example.lpsx.entity.School;
import com.example.lpsx.entity.OfficialAccountResource;
import com.example.lpsx.entity.PrimarySchool;
import com.example.lpsx.entity.ParentQa;
import com.example.lpsx.entity.LotteryGuide;
import com.example.lpsx.mapper.AdmissionInfoMapper;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import com.example.lpsx.mapper.ParentQaMapper;
import com.example.lpsx.mapper.LotteryGuideMapper;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.mapper.OfficialAccountResourceMapper;
import com.example.lpsx.service.HomeService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HomeServiceImpl implements HomeService {

    @Resource
    private SchoolMapper schoolMapper;

    @Resource
    private PrimarySchoolMapper primarySchoolMapper;

    @Resource
    private ParentQaMapper parentQaMapper;

    @Resource
    private LotteryGuideMapper lotteryGuideMapper;

    @Resource
    private AdmissionInfoMapper admissionInfoMapper;

    @Resource
    private OfficialAccountResourceMapper officialAccountResourceMapper;

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Long schoolCount = schoolMapper.selectCount(new LambdaQueryWrapper<>());
            result.put("schoolCount", schoolCount);
        } catch (Exception e) {
            log.error("查询学校总数失败", e);
            result.put("schoolCount", 0);
        }

        // 招生动态：取最近更新的 5 条（is_active=1，按 update_time 倒序）
        try {
            List<AdmissionInfo> admissions = admissionInfoMapper.selectList(
                    new LambdaQueryWrapper<AdmissionInfo>()
                            .eq(AdmissionInfo::getIsActive, 1)
                            .orderByDesc(AdmissionInfo::getUpdateTime)
                            .last("LIMIT 5"));
            List<Map<String, Object>> notices = admissions.stream().map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("schoolId", a.getSchoolId());
                m.put("schoolName", a.getSchoolName());
                return m;
            }).collect(Collectors.toList());
            result.put("notices", notices);
        } catch (Exception e) {
            log.error("查询招生动态失败", e);
            result.put("notices", new ArrayList<>());
        }

        try {
            List<PrimarySchool> schools = primarySchoolMapper.selectList(
                    new LambdaQueryWrapper<PrimarySchool>()
                            .eq(PrimarySchool::getTier, "一梯队")
                            .orderByAsc(PrimarySchool::getDistrict)
                            .orderByAsc(PrimarySchool::getName)
                            .last("LIMIT 12"));
            List<Map<String, Object>> hotSchools = schools.stream().map(this::toHotPrimarySchool).collect(Collectors.toList());
            result.put("hotSchools", hotSchools);
        } catch (Exception e) {
            log.error("查询热门学校失败", e);
            result.put("hotSchools", new ArrayList<>());
        }

        // 首页专题入口的数量与各专题列表使用相同的发布口径。
        try {
            Map<String, Long> topicStats = new LinkedHashMap<>();
            topicStats.put("parentQa", parentQaMapper.selectCount(
                    new LambdaQueryWrapper<ParentQa>().eq(ParentQa::getStatus, 1)));
            topicStats.put("lotteryGuide", lotteryGuideMapper.selectCount(
                    new LambdaQueryWrapper<LotteryGuide>().eq(LotteryGuide::getStatus, 1)));
            topicStats.put("hotSchools", primarySchoolMapper.selectCount(
                    new LambdaQueryWrapper<PrimarySchool>().eq(PrimarySchool::getTier, "一梯队")));
            result.put("topicStats", topicStats);
        } catch (Exception e) {
            log.error("查询首页专题数量失败", e);
            result.put("topicStats", Map.of("parentQa", 0L, "lotteryGuide", 0L, "hotSchools", 0L));
        }

        try {
            List<OfficialAccountResource> articles = officialAccountResourceMapper.selectList(
                    new LambdaQueryWrapper<OfficialAccountResource>()
                            .eq(OfficialAccountResource::getIsEnabled, 1)
                            .orderByAsc(OfficialAccountResource::getSortOrder)
                            .orderByDesc(OfficialAccountResource::getId)
                            .last("LIMIT 6"));
            result.put("hotArticles", articles);
        } catch (Exception e) {
            log.error("查询公众号热门文章失败", e);
            result.put("hotArticles", new ArrayList<>());
        }

        List<Map<String, String>> shortcuts = new ArrayList<>();
        shortcuts.add(createShortcut("择校测评", "📝", "/pages/assessment/assessment"));
        shortcuts.add(createShortcut("学校库", "🏫", "/pages/school/school"));
        shortcuts.add(createShortcut("我的收藏", "❤️", "/pages/user/user"));
        shortcuts.add(createShortcut("留资咨询", "📞", "/pages/lead/lead"));
        result.put("shortcuts", shortcuts);

        // 学段真实统计
        List<Map<String, Object>> stats = new ArrayList<>();
        stats.add(Map.of("label", "国际小学", "count",
                schoolMapper.selectCount(new LambdaQueryWrapper<School>().like(School::getGrades, "小学"))));
        stats.add(Map.of("label", "国际初中", "count",
                schoolMapper.selectCount(new LambdaQueryWrapper<School>().like(School::getGrades, "初中"))));
        stats.add(Map.of("label", "国际高中", "count",
                schoolMapper.selectCount(new LambdaQueryWrapper<School>().like(School::getGrades, "高中"))));
        result.put("schoolStats", stats);

        return result;
    }

    private Map<String, Object> toHotPrimarySchool(PrimarySchool school) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", school.getId());
        item.put("schoolId", school.getId());
        item.put("schoolSource", "primary");
        item.put("schoolLevel", "小学");
        item.put("name", school.getName());
        item.put("shortName", school.getShortName());
        item.put("logoUrl", school.getBackgroundImageUrl());
        item.put("district", school.getDistrict());
        item.put("tier", school.getTier());
        item.put("address", school.getAddress());
        item.put("schoolType", school.getSchoolType());
        return item;
    }

    @Override
    public List<Map<String, Object>> districtStats() {
        List<String> districts = Arrays.asList(
                "黄浦区", "普陀区", "杨浦区", "嘉定区",
                "浦东新区", "长宁区", "徐汇区", "闵行区"
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (String d : districts) {
            Long cnt = schoolMapper.selectCount(
                    new LambdaQueryWrapper<School>().eq(School::getDistrict, d));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("district", d);
            item.put("count", cnt);
            result.add(item);
        }
        return result;
    }

    private Map<String, String> createShortcut(String name, String icon, String path) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("icon", icon);
        item.put("path", path);
        return item;
    }
}
