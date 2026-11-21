package com.example.lpsx.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.GeneralHighSchool;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.entity.Kindergarten;
import com.example.lpsx.mapper.GeneralHighSchoolMapper;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import com.example.lpsx.mapper.KindergartenMapper;
import com.example.lpsx.vo.SchoolCompareDetailsVO;
import com.example.lpsx.vo.SchoolCompareRef;
import com.example.lpsx.vo.SchoolCompareRequest;
import com.example.lpsx.vo.SchoolCompareSearchPageVO;
import com.example.lpsx.vo.SchoolCompareVO;

import jakarta.annotation.Resource;

/** 体制内初中/高中学校搜索与批量对比。 */
@RestController
@RequestMapping("/api/school-compare")
public class SchoolCompareController {
    private static final int MAX_SEARCH_SIZE = 50;
    private static final int MAX_COMPARE_COUNT = 6;

    @Resource private JuniorHighSchoolMapper juniorMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreMapper;
    @Resource private GeneralHighSchoolMapper generalMapper;
    @Resource private KindergartenMapper kindergartenMapper;

    @GetMapping("/search")
    public ResponseEntity<Result<SchoolCompareSearchPageVO>> search(
            @RequestParam String level,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        String normalizedLevel = normalize(level);
        if (!"kindergarten".equals(normalizedLevel) && !"junior".equals(normalizedLevel) && !"high".equals(normalizedLevel)) {
            return badRequest("level 仅支持 kindergarten、junior 或 high");
        }
        if (!StringUtils.hasText(keyword)) {
            return badRequest("学校关键词不能为空");
        }
        String term = keyword.trim();
        if (term.length() > 50) {
            return badRequest("学校关键词不能超过50个字符");
        }
        if (page < 1 || page > 200 || size < 1 || size > MAX_SEARCH_SIZE) {
            return badRequest("page 范围为1-200，size 范围为1-50");
        }

        List<SchoolCompareVO> matched = switch (normalizedLevel) {
            case "kindergarten" -> searchKindergarten(term);
            case "junior" -> searchJunior(term);
            default -> searchHigh(term);
        };
        Comparator<SchoolCompareVO> ordering = Comparator
                .comparingInt((SchoolCompareVO school) -> matchRank(school, term))
                .thenComparing(SchoolCompareVO::getName, Comparator.nullsLast(String::compareTo))
                .thenComparing(SchoolCompareVO::getSchoolSource)
                .thenComparing(SchoolCompareVO::getId);
        matched.sort(ordering);

        int from = Math.min((page - 1) * size, matched.size());
        int to = Math.min(from + size, matched.size());
        List<SchoolCompareVO> records = new ArrayList<>(matched.subList(from, to));
        SchoolCompareSearchPageVO data = new SchoolCompareSearchPageVO(
                records, page, size, matched.size(), to < matched.size());
        return ResponseEntity.ok(Result.success(data));
    }

    @PostMapping("/details")
    public ResponseEntity<Result<SchoolCompareDetailsVO>> details(@RequestBody(required = false) SchoolCompareRequest body) {
        if (body == null || body.getSchools() == null || body.getSchools().isEmpty()) {
            return badRequest("请至少选择1所学校进行对比");
        }

        LinkedHashMap<String, SchoolCompareRef> unique = new LinkedHashMap<>();
        for (SchoolCompareRef ref : body.getSchools()) {
            if (ref == null || ref.getId() == null || ref.getId() <= 0 || !isDomesticSource(ref.getSchoolSource())) {
                return badRequest("学校来源或学校编号不正确");
            }
            String source = normalize(ref.getSchoolSource());
            ref.setSchoolSource(source);
            unique.putIfAbsent(source + ":" + ref.getId(), ref);
        }
        if (unique.size() > MAX_COMPARE_COUNT) {
            return badRequest("最多选择6所学校进行对比");
        }

        boolean hasJunior = unique.values().stream().anyMatch(ref -> "junior".equals(ref.getSchoolSource()));
        boolean hasKindergarten = unique.values().stream().anyMatch(ref -> "kindergarten".equals(ref.getSchoolSource()));
        boolean hasHigh = unique.values().stream().anyMatch(ref -> "score".equals(ref.getSchoolSource()) || "general".equals(ref.getSchoolSource()));
        if ((hasKindergarten ? 1 : 0) + (hasJunior ? 1 : 0) + (hasHigh ? 1 : 0) > 1) {
            return badRequest("不同学段不能混合对比");
        }

        List<Integer> juniorIds = idsFor(unique, "junior");
        List<Integer> scoreIds = idsFor(unique, "score");
        List<Integer> generalIds = idsFor(unique, "general");
        List<Integer> kindergartenIds = idsFor(unique, "kindergarten");
        Map<String, SchoolCompareVO> found = new HashMap<>();
        if (!juniorIds.isEmpty()) {
            juniorMapper.selectBatchIds(juniorIds).stream().map(this::fromJunior)
                    .forEach(school -> found.put(school.getSchoolSource() + ":" + school.getId(), school));
        }
        if (!scoreIds.isEmpty()) {
            scoreMapper.selectBatchIds(scoreIds).stream().map(this::fromScore)
                    .forEach(school -> found.put(school.getSchoolSource() + ":" + school.getId(), school));
        }
        if (!generalIds.isEmpty()) {
            generalMapper.selectBatchIds(generalIds).stream().map(this::fromGeneral)
                    .forEach(school -> found.put(school.getSchoolSource() + ":" + school.getId(), school));
        }
        if (!kindergartenIds.isEmpty()) {
            kindergartenMapper.selectBatchIds(kindergartenIds).stream().map(this::fromKindergarten)
                    .forEach(school -> found.put(school.getSchoolSource() + ":" + school.getId(), school));
        }

        List<SchoolCompareVO> ordered = new ArrayList<>();
        for (Map.Entry<String, SchoolCompareRef> entry : unique.entrySet()) {
            SchoolCompareVO school = found.get(entry.getKey());
            if (school == null) {
                SchoolCompareRef missing = entry.getValue();
                return ResponseEntity.status(404).body(Result.error(404,
                        "学校不存在: " + missing.getSchoolSource() + "/" + missing.getId()));
            }
            ordered.add(school);
        }
        String level = hasKindergarten ? "kindergarten" : (hasJunior ? "junior" : "high");
        return ResponseEntity.ok(Result.success(new SchoolCompareDetailsVO("domestic", level, ordered)));
    }

    private List<SchoolCompareVO> searchJunior(String term) {
        return new ArrayList<>(juniorMapper.selectList(new LambdaQueryWrapper<JuniorHighSchool>()
                        .and(q -> q.like(JuniorHighSchool::getName, term)
                                .or().like(JuniorHighSchool::getShortName, term)))
                .stream().map(this::fromJunior).toList());
    }

    private List<SchoolCompareVO> searchKindergarten(String term) {
        return new ArrayList<>(kindergartenMapper.selectList(new LambdaQueryWrapper<Kindergarten>()
                        .eq(Kindergarten::getIsActive, true)
                        .and(q -> q.like(Kindergarten::getName, term).or().like(Kindergarten::getShortName, term)))
                .stream().map(this::fromKindergarten).toList());
    }

    private List<SchoolCompareVO> searchHigh(String term) {
        List<SchoolCompareVO> result = new ArrayList<>();
        result.addAll(scoreMapper.selectList(new LambdaQueryWrapper<ScoreQueryToolSchool>()
                        .and(q -> q.like(ScoreQueryToolSchool::getName, term)
                                .or().like(ScoreQueryToolSchool::getShortName, term)))
                .stream().map(this::fromScore).toList());
        result.addAll(generalMapper.selectList(new LambdaQueryWrapper<GeneralHighSchool>()
                        .and(q -> q.like(GeneralHighSchool::getName, term)
                                .or().like(GeneralHighSchool::getShortName, term)))
                .stream().map(this::fromGeneral).toList());
        return result;
    }

    private SchoolCompareVO fromJunior(JuniorHighSchool school) {
        return school(school.getId(), "junior", "初中", school.getName(), school.getShortName(),
                school.getDistrict(), school.getSchoolType(), school.getTuitionMin(), school.getTuitionMax(),
                school.getBoardingInfo(), school.getBackgroundImageUrl(), school.getAddress(), school.getDescription(),
                school.getOfficialWebsite(), school.getSchedule(), school.getTeachingFeatures(), school.getCampusArea());
    }

    private SchoolCompareVO fromScore(ScoreQueryToolSchool school) {
        return school(school.getId(), "score", "高中", school.getName(), school.getShortName(),
                school.getDistrict(), school.getSchoolType(), school.getTuitionMin(), school.getTuitionMax(),
                school.getBoardingInfo(), school.getLogo(), school.getAddress(), school.getDescription(),
                school.getOfficialWebsite(), school.getSchedule(), school.getTeachingFeatures(), school.getCampusArea());
    }

    private SchoolCompareVO fromGeneral(GeneralHighSchool school) {
        return school(school.getId(), "general", "高中", school.getName(), school.getShortName(),
                school.getDistrict(), school.getSchoolType(), school.getTuitionMin(), school.getTuitionMax(),
                school.getBoardingInfo(), school.getBackgroundImageUrl(), school.getAddress(), school.getDescription(),
                school.getOfficialWebsite(), school.getSchedule(), school.getTeachingFeatures(), school.getCampusArea());
    }

    private SchoolCompareVO fromKindergarten(Kindergarten school) {
        return school(school.getId(), "kindergarten", "幼儿园", school.getName(), school.getShortName(),
                school.getDistrict(), school.getSchoolType(), school.getTuitionMin(), school.getTuitionMax(),
                school.getCareType(), school.getBackgroundImageUrl(), school.getAddress(), school.getDescription(),
                school.getOfficialWebsite(), school.getSchedule(), school.getTeachingFeatures(), school.getCampusArea());
    }

    private SchoolCompareVO school(Integer id, String source, String level, String name, String shortName,
                                   String district, String schoolType, java.math.BigDecimal tuitionMin,
                                   java.math.BigDecimal tuitionMax, String boardingInfo, String logoUrl,
                                   String address, String description, String website, String schedule,
                                   String features, java.math.BigDecimal campusArea) {
        String nature = schoolType;
        if (schoolType != null && schoolType.contains("公办")) nature = "公办";
        if (schoolType != null && schoolType.contains("民办")) nature = "民办";
        return new SchoolCompareVO(id, compareKey(source, id), source, source, name, shortName, district,
                level, schoolType, nature, tuitionMin, tuitionMax, boardingInfo, logoUrl, address,
                description, website, schedule, features, campusArea);
    }

    private int matchRank(SchoolCompareVO school, String term) {
        String name = normalize(school.getName());
        String shortName = normalize(school.getShortName());
        String keyword = term.toLowerCase(Locale.ROOT);
        if (keyword.equals(name) || keyword.equals(shortName)) return 0;
        if (name.startsWith(keyword) || shortName.startsWith(keyword)) return 1;
        return 2;
    }

    private List<Integer> idsFor(Map<String, SchoolCompareRef> unique, String source) {
        return unique.values().stream().filter(ref -> source.equals(ref.getSchoolSource()))
                .map(SchoolCompareRef::getId).toList();
    }

    private String compareKey(String source, Integer id) {
        return "domestic:" + source + ":" + id;
    }

    private boolean isDomesticSource(String source) {
        String normalized = normalize(source);
        return "kindergarten".equals(normalized) || "junior".equals(normalized)
                || "score".equals(normalized) || "general".equals(normalized);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private <T> ResponseEntity<Result<T>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Result.error(400, message));
    }
}
