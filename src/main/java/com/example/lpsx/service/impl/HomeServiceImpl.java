package com.example.lpsx.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.AdmissionInfo;
import com.example.lpsx.entity.GeneralHighSchool;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.entity.LotteryGuide;
import com.example.lpsx.entity.OfficialAccountResource;
import com.example.lpsx.entity.ParentQa;
import com.example.lpsx.entity.PrimarySchool;
import com.example.lpsx.entity.School;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.mapper.AdmissionInfoMapper;
import com.example.lpsx.mapper.GeneralHighSchoolMapper;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.LotteryGuideMapper;
import com.example.lpsx.mapper.OfficialAccountResourceMapper;
import com.example.lpsx.mapper.ParentQaMapper;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.mapper.SchoolReactionMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import com.example.lpsx.mapper.UserFavoriteMapper;
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

    @Resource
    private UserFavoriteMapper userFavoriteMapper;

    @Resource
    private SchoolReactionMapper schoolReactionMapper;

    @Resource
    private JuniorHighSchoolMapper juniorHighSchoolMapper;

    @Resource
    private ScoreQueryToolSchoolMapper scoreQueryToolSchoolMapper;

    @Resource
    private GeneralHighSchoolMapper generalHighSchoolMapper;

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
            // 热度 = 收藏数 + 评价数（按 school_source + school_id 统计，跨学段共用）
            Map<String, Long> heatMap = loadHeatMap();

            // 候选池 = 小学/初中/高中（一梯队）三学段合并，基准顺序：区→校名。
            // 一般高中表无 tier 字段，仅当其中某校有热度时才纳入候选。
            List<HotCandidate> candidates = new ArrayList<>();
            primarySchoolMapper.selectList(
                    new LambdaQueryWrapper<PrimarySchool>()
                            .eq(PrimarySchool::getTier, "一梯队")
                            .orderByAsc(PrimarySchool::getDistrict)
                            .orderByAsc(PrimarySchool::getName))
                    .forEach(s -> candidates.add(new HotCandidate("primary", s.getId(), s.getDistrict(), s.getName(),
                            s.getShortName(), s.getBackgroundImageUrl(), s.getAddress(), s.getSchoolType(), s.getTier(), heatMap)));
            juniorHighSchoolMapper.selectList(
                    new LambdaQueryWrapper<JuniorHighSchool>()
                            .eq(JuniorHighSchool::getTier, "一梯队")
                            .orderByAsc(JuniorHighSchool::getDistrict)
                            .orderByAsc(JuniorHighSchool::getName))
                    .forEach(s -> candidates.add(new HotCandidate("junior", s.getId(), s.getDistrict(), s.getName(),
                            s.getShortName(), s.getBackgroundImageUrl(), s.getAddress(), s.getSchoolType(), s.getTier(), heatMap)));
            scoreQueryToolSchoolMapper.selectList(
                    new LambdaQueryWrapper<ScoreQueryToolSchool>()
                            .eq(ScoreQueryToolSchool::getTier, "一梯队")
                            .orderByAsc(ScoreQueryToolSchool::getDistrict)
                            .orderByAsc(ScoreQueryToolSchool::getName))
                    .forEach(s -> candidates.add(new HotCandidate("score", s.getId(), s.getDistrict(), s.getName(),
                            s.getShortName(), s.getLogo(), s.getAddress(), s.getSchoolType(), s.getTier(), heatMap)));
            generalHighSchoolMapper.selectList(
                    new LambdaQueryWrapper<GeneralHighSchool>()
                            .orderByAsc(GeneralHighSchool::getDistrict)
                            .orderByAsc(GeneralHighSchool::getName))
                    .forEach(s -> {
                        if (heatOf(heatMap, "general", s.getId()) > 0) {
                            candidates.add(new HotCandidate("general", s.getId(), s.getDistrict(), s.getName(),
                                    s.getShortName(), s.getBackgroundImageUrl(), s.getAddress(), s.getSchoolType(), null, heatMap));
                        }
                    });

            // 按热度倒序（稳定排序，热度相同保持基准顺序），取前 12
            List<HotCandidate> ordered = candidates.stream()
                    .sorted(Comparator.comparingLong(HotCandidate::heat).reversed())
                    .limit(12)
                    .collect(Collectors.toList());
            List<Map<String, Object>> hotSchools = ordered.stream().map(this::toHotSchool).collect(Collectors.toList());
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
                    new LambdaQueryWrapper<PrimarySchool>().eq(PrimarySchool::getTier, "一梯队"))
                    + juniorHighSchoolMapper.selectCount(
                    new LambdaQueryWrapper<JuniorHighSchool>().eq(JuniorHighSchool::getTier, "一梯队"))
                    + scoreQueryToolSchoolMapper.selectCount(
                    new LambdaQueryWrapper<ScoreQueryToolSchool>().eq(ScoreQueryToolSchool::getTier, "一梯队")));
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

    /** 热门候选学校（跨学段统一表示，展示字段在构建时从各学段实体捕获，避免 N+1 查询） */
    private static final class HotCandidate {
        final String source;
        final Integer id;
        final String district;
        final String name;
        final String shortName;
        final String logoUrl;
        final String address;
        final String schoolType;
        final String tier;
        final long heat;

        HotCandidate(String source, Integer id, String district, String name, String shortName,
                     String logoUrl, String address, String schoolType, String tier, Map<String, Long> heatMap) {
            this.source = source;
            this.id = id;
            this.district = district;
            this.name = name;
            this.shortName = shortName;
            this.logoUrl = logoUrl;
            this.address = address;
            this.schoolType = schoolType;
            this.tier = tier;
            this.heat = heatOf(heatMap, source, id);
        }

        long heat() {
            return heat;
        }
    }

    /** 热度键：school_source + ':' + school_id */
    private static String heatKey(String source, Integer id) {
        return source + ":" + id;
    }

    private static long heatOf(Map<String, Long> heatMap, String source, Integer id) {
        return heatMap.getOrDefault(heatKey(source, id), 0L);
    }

    /** 汇总收藏 + 评价，按 (school_source, school_id) 计算每所学校的热度 */
    private Map<String, Long> loadHeatMap() {
        Map<String, Long> heat = new LinkedHashMap<>();
        accumulate(heat, userFavoriteMapper.selectFavoriteCountsBySchool());
        accumulate(heat, schoolReactionMapper.selectReactionCountsBySchool());
        return heat;
    }

    private void accumulate(Map<String, Long> heat, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            Object src = row.get("schoolSource");
            Object id = row.get("schoolId");
            Object cnt = row.get("cnt");
            if (src != null && id instanceof Number && cnt instanceof Number) {
                heat.merge(heatKey(String.valueOf(src), ((Number) id).intValue()),
                        ((Number) cnt).longValue(), Long::sum);
            }
        }
    }

    private Map<String, Object> toHotSchool(HotCandidate c) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", c.id);
        item.put("schoolId", c.id);
        item.put("schoolSource", c.source);
        item.put("schoolLevel", levelOf(c.source));
        item.put("name", c.name);
        item.put("district", c.district);
        item.put("logoUrl", c.logoUrl);
        item.put("shortName", c.shortName);
        item.put("address", c.address);
        item.put("schoolType", c.schoolType);
        item.put("tier", c.tier);
        return item;
    }

    private String levelOf(String source) {
        switch (source) {
            case "primary": return "小学";
            case "junior": return "初中";
            case "score":
            case "general": return "高中";
            default: return "";
        }
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
