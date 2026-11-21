package com.example.lpsx.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.Blessing;
import com.example.lpsx.entity.User;
import com.example.lpsx.entity.School;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.entity.GeneralHighSchool;
import com.example.lpsx.entity.PrimarySchool;
import com.example.lpsx.entity.Kindergarten;
import com.example.lpsx.mapper.BlessingMapper;
import com.example.lpsx.mapper.UserMapper;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.GeneralHighSchoolMapper;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import com.example.lpsx.mapper.KindergartenMapper;
import com.example.lpsx.vo.BlessingListVO;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 中考祝福墙接口
 *
 * 发布祝福（需登录，自动带出微信昵称/头像）：
 *   POST /api/blessing    {"content": "张三 祝福 李四 考上 复旦"}
 *
 * 查看祝福（公开，无需登录）：
 *   GET  /api/blessing/list?page=1&size=20   分页返回已发布祝福（按时间倒序）
 *
 * 说明：无点赞、无评论，仅"发布 + 展示"。
 */
@RestController
@RequestMapping("/api/blessing")
public class BlessingController {

    /** 祝福内容最大长度 */
    private static final int MAX_CONTENT_LEN = 200;
    private static final Set<String> SCHOOL_SOURCES = Set.of("international", "score", "junior", "general", "primary", "kindergarten");

    @Resource
    private BlessingMapper blessingMapper;

    @Resource
    private UserMapper userMapper;

    @Resource private SchoolMapper schoolMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreMapper;
    @Resource private JuniorHighSchoolMapper juniorMapper;
    @Resource private GeneralHighSchoolMapper generalMapper;
    @Resource private PrimarySchoolMapper primaryMapper;
    @Resource private KindergartenMapper kindergartenMapper;

    /**
     * 发布祝福（需登录）
     */
    @PostMapping
    public Result<Blessing> create(@RequestBody Map<String, String> body,
                                   HttpServletRequest request) {
        String content = body == null ? null : body.get("content");
        String schoolSource = body == null ? null : body.get("schoolSource");
        Integer schoolId = parsePositiveInt(body == null ? null : body.get("schoolId"));
        if (!StringUtils.hasText(content)) {
            return Result.error("祝福内容不能为空");
        }
        content = content.trim();
        if (content.length() > MAX_CONTENT_LEN) {
            return Result.error("祝福内容最多 " + MAX_CONTENT_LEN + " 字");
        }
        if (!SCHOOL_SOURCES.contains(schoolSource) || schoolId == null) {
            return Result.error("请选择要祝福的学校");
        }
        String schoolName = resolveSchoolName(schoolSource, schoolId);
        if (!StringUtils.hasText(schoolName)) return Result.error("学校不存在");

        // 从 JWT 拦截器注入的 userId 取出用户（仅 SELECT，不改动其他表）
        Integer userId = (Integer) request.getAttribute("userId");
        User user = userId != null ? userMapper.selectById(userId) : null;

        Blessing blessing = new Blessing();
        String openid = user != null ? user.getOpenid() : "";
        blessing.setOpenid(openid);
        blessing.setCloudOpenid(openid);
        blessing.setNickname(user != null && StringUtils.hasText(user.getNickname())
                ? user.getNickname() : "匿名用户");
        blessing.setAvatar(user != null ? user.getAvatar() : "");
        blessing.setSchoolSource(schoolSource);
        blessing.setSchoolId(schoolId);
        blessing.setSchoolName(schoolName);
        blessing.setContent(content);
        blessing.setStatus(1); // 1=已发布，发布即可见
        blessing.setCreateTime(LocalDateTime.now());
        blessingMapper.insert(blessing);
        return Result.success(blessing);
    }

    /**
     * 祝福墙列表（公开，分页，仅已发布）
     */
    @GetMapping("/list")
    public Result<Page<BlessingListVO>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String schoolSource,
                                       @RequestParam(required = false) Integer schoolId) {
        if (page < 1) page = 1;
        if (size < 1 || size > 50) size = 20;
        LambdaQueryWrapper<Blessing> qw = new LambdaQueryWrapper<>();
        qw.eq(Blessing::getStatus, 1);
        if (StringUtils.hasText(schoolSource) || schoolId != null) {
            if (!SCHOOL_SOURCES.contains(schoolSource) || schoolId == null || schoolId <= 0) {
                return Result.error("学校参数不正确");
            }
            qw.eq(Blessing::getSchoolSource, schoolSource).eq(Blessing::getSchoolId, schoolId);
        } else {
            // 中考查分页展示全局祝福墙：纳入所有学校来源，但学校名称为空的旧数据不可展示。
            qw.isNotNull(Blessing::getSchoolName).ne(Blessing::getSchoolName, "");
        }
        qw.orderByDesc(Blessing::getCreateTime).orderByDesc(Blessing::getId);
        Page<Blessing> rows = blessingMapper.selectPage(new Page<>(page, size), qw);
        Page<BlessingListVO> response = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        response.setRecords(rows.getRecords().stream().map(this::toListVO).toList());
        return Result.success(response);
    }

    private BlessingListVO toListVO(Blessing blessing) {
        return new BlessingListVO(blessing.getId(), blessing.getNickname(), blessing.getAvatar(),
                blessing.getSchoolSource(), blessing.getSchoolId(), blessing.getSchoolName(),
                blessing.getContent(), blessing.getCreateTime());
    }

    private Integer parsePositiveInt(String value) {
        try { int id = Integer.parseInt(value); return id > 0 ? id : null; }
        catch (Exception ignored) { return null; }
    }

    private String resolveSchoolName(String source, Integer id) {
        return switch (source) {
            case "international" -> { School row = schoolMapper.selectById(id); yield row == null ? null : row.getName(); }
            case "score" -> { ScoreQueryToolSchool row = scoreMapper.selectById(id); yield row == null ? null : row.getName(); }
            case "junior" -> { JuniorHighSchool row = juniorMapper.selectById(id); yield row == null ? null : row.getName(); }
            case "general" -> { GeneralHighSchool row = generalMapper.selectById(id); yield row == null ? null : row.getName(); }
            case "primary" -> { PrimarySchool row = primaryMapper.selectById(id); yield row == null ? null : row.getName(); }
            case "kindergarten" -> { Kindergarten row = kindergartenMapper.selectById(id); yield row == null ? null : row.getName(); }
            default -> null;
        };
    }
}
