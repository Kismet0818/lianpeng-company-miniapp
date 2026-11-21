package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.mapper.*;
import com.example.lpsx.vo.SchoolReactionVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/school-reactions")
public class SchoolReactionController {
    private static final Set<String> SOURCES = Set.of("international", "score", "junior", "general", "primary", "kindergarten");
    @Resource private SchoolReactionMapper reactionMapper;
    @Resource private SchoolMapper schoolMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreMapper;
    @Resource private JuniorHighSchoolMapper juniorMapper;
    @Resource private GeneralHighSchoolMapper generalMapper;
    @Resource private PrimarySchoolMapper primaryMapper;
    @Resource private KindergartenMapper kindergartenMapper;

    @GetMapping("/{schoolId}")
    public Result<SchoolReactionVO> get(@PathVariable Integer schoolId,
                                        @RequestParam(defaultValue = "international") String schoolSource,
                                        @RequestAttribute("userId") Integer userId) {
        String source = normalizeSource(schoolSource);
        if (source == null) return Result.error("学校来源不正确");
        if (!schoolExists(source, schoolId)) return Result.error("学校不存在");
        return Result.success(build(source, schoolId, userId));
    }

    @PostMapping("/{schoolId}")
    public Result<SchoolReactionVO> submit(@PathVariable Integer schoolId,
                                           @RequestBody Map<String, Object> body,
                                           @RequestAttribute("userId") Integer userId) {
        String source = normalizeSource(body == null ? null : String.valueOf(body.getOrDefault("schoolSource", "international")));
        if (source == null) return Result.error("学校来源不正确");
        if (!schoolExists(source, schoolId)) return Result.error("学校不存在");
        Integer type = number(body == null ? null : body.get("reactionType"));
        if (type == null || type < 1 || type > 5) return Result.error("表情编号必须是1到5");
        reactionMapper.upsert(source, schoolId, userId, type);
        return Result.success(build(source, schoolId, userId));
    }

    private SchoolReactionVO build(String source, Integer schoolId, Integer userId) {
        SchoolReactionVO vo = new SchoolReactionVO();
        for (int i = 1; i <= 5; i++) vo.getCounts().put(String.valueOf(i), 0L);
        for (Map<String, Object> row : reactionMapper.selectCounts(source, schoolId)) {
            Object type = row.get("reactionType");
            Object count = row.get("reactionCount");
            if (type != null && count != null) vo.getCounts().put(String.valueOf(type), ((Number) count).longValue());
        }
        vo.setReactionType(reactionMapper.selectMine(source, schoolId, userId));
        return vo;
    }

    private String normalizeSource(String source) {
        if (source == null) return null;
        String value = source.trim().toLowerCase();
        return SOURCES.contains(value) ? value : null;
    }

    private boolean schoolExists(String source, Integer id) {
        if (id == null || id <= 0) return false;
        return switch (source) {
            case "international" -> schoolMapper.selectById(id) != null;
            case "score" -> scoreMapper.selectById(id) != null;
            case "junior" -> juniorMapper.selectById(id) != null;
            case "general" -> generalMapper.selectById(id) != null;
            case "primary" -> primaryMapper.selectById(id) != null;
            case "kindergarten" -> kindergartenMapper.selectById(id) != null;
            default -> false;
        };
    }

    private Integer number(Object value) {
        try { return value == null ? null : Integer.valueOf(String.valueOf(value)); }
        catch (NumberFormatException e) { return null; }
    }
}
