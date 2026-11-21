package com.example.lpsx.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.ScoreQueryTool;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.entity.ScoreQueryToolSchoolImage;
import com.example.lpsx.entity.ScoreVolunteer;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.ScoreQueryToolMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolImageMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import com.example.lpsx.mapper.ScoreVolunteerMapper;
import com.example.lpsx.vo.ScoreQueryToolSchoolListVO;
import com.example.lpsx.vo.PageResultVO;
import com.example.lpsx.vo.ScoreSchoolSearchVO;

import jakarta.annotation.Resource;

/**
 * 查分工具接口 — 公开访问（无需登录）
 * <p>
 * 1. GET /api/score-query/meta               筛选条件（学年/目标区/目标初中）
 * 2. GET /api/score-query/region             名额到区查询
 * 3. GET /api/score-query/school             名额到校查询
 * 4. GET /api/score-query/schools/{id}       高中学校详情
 * 5. GET /api/score-query/schools/district-key 按区查询区重点学校
 */
@RestController
@RequestMapping("/api/score-query")
public class ScoreQueryController {

    private static final String DEFAULT_YEAR = "2025-2026";
    private static final Pattern YEAR_PATTERN = Pattern.compile("^\\d{4}-\\d{4}$");

    @Resource
    private ScoreQueryToolMapper scoreQueryToolMapper;

    @Resource
    private ScoreQueryToolSchoolMapper scoreQueryToolSchoolMapper;

    @Resource
    private ScoreQueryToolSchoolImageMapper scoreQueryToolSchoolImageMapper;

    @Resource
    private ScoreVolunteerMapper scoreVolunteerMapper;

    @Resource
    private JuniorHighSchoolMapper juniorHighSchoolMapper;

    /**
     * 筛选条件：学年 / 目标区 / 目标初中
     */
    @GetMapping("/meta")
    public Result<Map<String, Object>> meta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("years", scoreQueryToolMapper.selectYears());
        meta.put("districts", scoreQueryToolMapper.selectTargetDistricts());
        meta.put("juniorSchools", scoreQueryToolMapper.selectJuniorSchools());
        return Result.success(meta);
    }

    /**
     * 名额到区查询（target_type=2）
     *
     * @param district 目标区（如 徐汇区）
     * @param year     学年（如 2025-2026）
     */
    @GetMapping("/region")
    public ResponseEntity<Result<List<ScoreQueryTool>>> region(@RequestParam(required = false) String district,
                                               @RequestParam(required = false) String year,
                                               @RequestParam(required = false) Integer highSchoolId) {
        String resolvedYear = resolveYear(year);
        if (resolvedYear == null) {
            return ResponseEntity.badRequest().body(Result.error(400, "年份格式错误或暂无该年份数据"));
        }
        LambdaQueryWrapper<ScoreQueryTool> qw = new LambdaQueryWrapper<>();
        qw.eq(ScoreQueryTool::getTargetType, 2);
        if (StringUtils.hasText(district)) {
            qw.eq(ScoreQueryTool::getTargetDistrict, district);
        }
        if (highSchoolId != null) {
            qw.eq(ScoreQueryTool::getHighSchoolId, highSchoolId);
        }
        qw.eq(ScoreQueryTool::getAcademicYear, resolvedYear)
                .orderByAsc(ScoreQueryTool::getHighSchoolName)
                .orderByAsc(ScoreQueryTool::getTargetDistrict)
                .orderByAsc(ScoreQueryTool::getId);
        return ResponseEntity.ok(Result.success(scoreQueryToolMapper.selectList(qw)));
    }

    /**
     * 名额到校查询（target_type=1）
     *
     * @param school 目标初中名称（模糊匹配）
     * @param year   学年（如 2025-2026）
     */
    @GetMapping("/school")
    public ResponseEntity<Result<List<ScoreQueryTool>>> school(@RequestParam(required = false) String school,
                                               @RequestParam(required = false) String year,
                                               @RequestParam(required = false) String district,
                                               @RequestParam(required = false) Integer highSchoolId) {
        String resolvedYear = resolveYear(year);
        if (resolvedYear == null) {
            return ResponseEntity.badRequest().body(Result.error(400, "年份格式错误或暂无该年份数据"));
        }
        LambdaQueryWrapper<ScoreQueryTool> qw = new LambdaQueryWrapper<>();
        qw.eq(ScoreQueryTool::getTargetType, 1);
        if (StringUtils.hasText(school)) {
            qw.like(ScoreQueryTool::getTargetSchoolName, school);
        }
        if (StringUtils.hasText(district)) {
            qw.eq(ScoreQueryTool::getTargetDistrict, district.trim());
        }
        if (highSchoolId != null) {
            qw.eq(ScoreQueryTool::getHighSchoolId, highSchoolId);
        }
        qw.eq(ScoreQueryTool::getAcademicYear, resolvedYear)
                .orderByAsc(ScoreQueryTool::getHighSchoolName)
                .orderByAsc(ScoreQueryTool::getTargetDistrict)
                .orderByAsc(ScoreQueryTool::getTargetSchoolName)
                .orderByAsc(ScoreQueryTool::getId);
        return ResponseEntity.ok(Result.success(scoreQueryToolMapper.selectList(qw)));
    }

    /**
     * 查分详情入口的统一学校检索。high/senior 查询查分高中，junior 查询初中库。
     */
    @GetMapping("/schools/search")
    public ResponseEntity<Result<PageResultVO<ScoreSchoolSearchVO>>> searchSchools(
            @RequestParam String level,
            @RequestParam String name,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        String normalizedLevel = StringUtils.hasText(level) ? level.trim().toLowerCase() : "";
        if ("senior".equals(normalizedLevel)) {
            normalizedLevel = "high";
        }
        if (!"junior".equals(normalizedLevel) && !"high".equals(normalizedLevel)) {
            return ResponseEntity.badRequest().body(Result.error(400, "level 仅支持 junior、high（兼容 senior）"));
        }
        if (!StringUtils.hasText(name)) {
            return ResponseEntity.badRequest().body(Result.error(400, "学校名称不能为空"));
        }
        String keyword = name.trim();
        if (keyword.length() > 100) {
            return ResponseEntity.badRequest().body(Result.error(400, "学校名称不能超过100个字符"));
        }
        if (page < 1 || page > 200 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().body(Result.error(400, "page 范围为1-200，size 范围为1-100"));
        }

        if ("high".equals(normalizedLevel)) {
            Page<ScoreQueryToolSchool> resultPage = scoreQueryToolSchoolMapper.selectPage(
                    Page.of(page, size),
                    new LambdaQueryWrapper<ScoreQueryToolSchool>()
                            .select(ScoreQueryToolSchool::getId, ScoreQueryToolSchool::getName,
                                    ScoreQueryToolSchool::getShortName, ScoreQueryToolSchool::getDistrict)
                            .and(q -> q.like(ScoreQueryToolSchool::getName, keyword)
                                    .or().like(ScoreQueryToolSchool::getShortName, keyword))
                            .orderByAsc(ScoreQueryToolSchool::getName)
                            .orderByAsc(ScoreQueryToolSchool::getId));
            List<ScoreSchoolSearchVO> records = resultPage.getRecords().stream()
                    .map(s -> new ScoreSchoolSearchVO(s.getId(), s.getName(), s.getShortName(),
                            s.getDistrict(), "high", "score"))
                    .toList();
            return ResponseEntity.ok(Result.success(new PageResultVO<>(records, resultPage.getTotal(), page, size,
                    resultPage.getPages())));
        }

        Page<JuniorHighSchool> resultPage = juniorHighSchoolMapper.selectPage(
                Page.of(page, size),
                new LambdaQueryWrapper<JuniorHighSchool>()
                        .select(JuniorHighSchool::getId, JuniorHighSchool::getName,
                                JuniorHighSchool::getShortName, JuniorHighSchool::getDistrict)
                        .and(q -> q.like(JuniorHighSchool::getName, keyword)
                                .or().like(JuniorHighSchool::getShortName, keyword))
                        .orderByAsc(JuniorHighSchool::getName)
                        .orderByAsc(JuniorHighSchool::getId));
        List<ScoreSchoolSearchVO> records = resultPage.getRecords().stream()
                .map(s -> new ScoreSchoolSearchVO(s.getId(), s.getName(), s.getShortName(),
                        s.getDistrict(), "junior", "junior"))
                .toList();
        return ResponseEntity.ok(Result.success(new PageResultVO<>(records, resultPage.getTotal(), page, size,
                resultPage.getPages())));
    }

    /**
     * 按所属区查询区重点学校。
     * <p>
     * 当前业务数据以 ID 75 为分界：从 ID 75 开始（即 ID &gt;= 75）且已配置学校简称的记录为区重点。
     * 这里不使用 school_type，因为该字段表示公办/民办，不是学校级别。
     *
     * @param district 所属区（必填，如 杨浦区）
     */
    @GetMapping("/schools/district-key")
    public Result<List<ScoreQueryToolSchoolListVO>> districtKeySchools(@RequestParam String district) {
        if (!StringUtils.hasText(district)) {
            return Result.error("请选择区县");
        }

        LambdaQueryWrapper<ScoreQueryToolSchool> qw = new LambdaQueryWrapper<>();
        qw.select(ScoreQueryToolSchool::getId,
                        ScoreQueryToolSchool::getName,
                        ScoreQueryToolSchool::getShortName,
                        ScoreQueryToolSchool::getLogo,
                        ScoreQueryToolSchool::getDistrict,
                        ScoreQueryToolSchool::getTier)
                .eq(ScoreQueryToolSchool::getDistrict, district.trim())
                .ge(ScoreQueryToolSchool::getId, 75)
                .isNotNull(ScoreQueryToolSchool::getShortName)
                .ne(ScoreQueryToolSchool::getShortName, "")
                .orderByAsc(ScoreQueryToolSchool::getId);
        List<ScoreQueryToolSchoolListVO> schools = scoreQueryToolSchoolMapper.selectList(qw).stream()
                .map(school -> new ScoreQueryToolSchoolListVO(
                        school.getId(),
                        school.getName(),
                        school.getShortName(),
                        school.getLogo(),
                        school.getDistrict(),
                        school.getTier()))
                .toList();
        return Result.success(schools);
    }

    /**
     * 高中学校详情（供查分结果点击查看学校信息）
     */
    @GetMapping("/schools/{id}")
    public ResponseEntity<Result<Map<String, Object>>> schoolDetail(@PathVariable Integer id) {
        ScoreQueryToolSchool school = scoreQueryToolSchoolMapper.selectById(id);
        if (school == null) {
            return ResponseEntity.status(404).body(Result.error(404, "高中学校不存在"));
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", school.getId());
        detail.put("name", school.getName());
        detail.put("shortName", school.getShortName());
        detail.put("logo", school.getLogo());
        detail.put("district", school.getDistrict());
        detail.put("tuitionMin", school.getTuitionMin());
        detail.put("tuitionMax", school.getTuitionMax());
        detail.put("boardingInfo", school.getBoardingInfo());
        detail.put("schoolType", school.getSchoolType());
        detail.put("tier", school.getTier());
        detail.put("address", school.getAddress());
        detail.put("description", school.getDescription());
        detail.put("officialWebsite", school.getOfficialWebsite());
        detail.put("schedule", school.getSchedule());
        detail.put("teachingFeatures", school.getTeachingFeatures());
        detail.put("campusArea", school.getCampusArea());
        detail.put("level", "high");
        detail.put("schoolSource", "score");
        return ResponseEntity.ok(Result.success(detail));
    }

    /**
     * 高中学校风光图列表（按 sort_order 升序）
     */
    @GetMapping("/schools/{id}/images")
    public Result<List<ScoreQueryToolSchoolImage>> schoolImages(@PathVariable Integer id) {
        LambdaQueryWrapper<ScoreQueryToolSchoolImage> qw = new LambdaQueryWrapper<>();
        qw.eq(ScoreQueryToolSchoolImage::getSchoolId, id)
                .orderByAsc(ScoreQueryToolSchoolImage::getSortOrder);
        return Result.success(scoreQueryToolSchoolImageMapper.selectList(qw));
    }

    /**
     * 自主招生查询（module_type=2）
     *
     * @param district 招生目标区（考生所在区，如 闵行区）
     * @param year     学年（如 2025-2026）
     */
    @GetMapping("/autonomous")
    public Result<List<ScoreVolunteer>> autonomous(@RequestParam(required = false) String district,
                                                   @RequestParam(required = false) String year) {
        String resolvedYear = resolveYear(year);
        if (resolvedYear == null) {
            return Result.error(400, "年份格式错误或暂无该年份数据");
        }
        LambdaQueryWrapper<ScoreVolunteer> qw = new LambdaQueryWrapper<>();
        qw.eq(ScoreVolunteer::getModuleType, 2);
        if (StringUtils.hasText(district)) {
            qw.eq(ScoreVolunteer::getTargetDistrict, district);
        }
        qw.eq(ScoreVolunteer::getAcademicYear, resolvedYear);
        // 录取线从高到低
        qw.orderByDesc(ScoreVolunteer::getMinScore);
        return Result.success(scoreVolunteerMapper.selectList(qw));
    }

    private String resolveYear(String year) {
        String resolved = StringUtils.hasText(year) ? year.trim() : DEFAULT_YEAR;
        if (!YEAR_PATTERN.matcher(resolved).matches()) {
            return null;
        }
        return scoreQueryToolMapper.selectYears().contains(resolved) ? resolved : null;
    }
}
