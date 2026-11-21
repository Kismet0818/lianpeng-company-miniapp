package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.*;
import com.example.lpsx.mapper.*;
import com.example.lpsx.vo.AdminUserVO;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    @Resource private UserMapper userMapper;
    @Resource private UserFavoriteMapper favoriteMapper;
    @Resource private CustomerLeadMapper leadMapper;
    @Resource private SchoolReactionMapper reactionMapper;
    @Resource private BlessingMapper blessingMapper;
    @Resource private UserActivityMapper activityMapper;
    @Resource private SchoolMapper schoolMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreSchoolMapper;
    @Resource private JuniorHighSchoolMapper juniorSchoolMapper;
    @Resource private GeneralHighSchoolMapper generalSchoolMapper;
    @Resource private PrimarySchoolMapper primarySchoolMapper;

    @GetMapping
    public Result<Page<AdminUserVO>> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        // 客户管理只展示已授权手机号的用户；同手机号仅展示最早创建的主用户。
        // 未授权用户仍可作为登录流程的临时技术记录存在，但不计入老板的客户名单。
        query.isNotNull(User::getPhone)
                .ne(User::getPhone, "")
                .inSql(User::getId,
                        "SELECT MIN(id) FROM `user` WHERE phone IS NOT NULL AND phone <> '' GROUP BY phone");
        if (status != null) query.eq(User::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(q -> q.like(User::getNickname, value).or().like(User::getPhone, value)
                    .or().like(User::getOpenid, value));
            try { query.or().eq(User::getId, Integer.valueOf(value)); } catch (NumberFormatException ignored) {}
        }
        query.orderByDesc(User::getLastActiveTime).orderByDesc(User::getCreateTime);
        Page<User> users = userMapper.selectPage(new Page<>(safePage, safeSize), query);
        Page<AdminUserVO> result = new Page<>(users.getCurrent(), users.getSize(), users.getTotal());
        result.setRecords(users.getRecords().stream().map(this::toVo).toList());
        return Result.success(result);
    }

    /** 新增人数按首次创建时间统计；访问者含未授权，授权用户按手机号去重。 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(userMapper.selectUsageStats());
    }

    @GetMapping("/{id}")
    public Result<AdminUserVO> detail(@PathVariable Integer id) {
        User user = userMapper.selectById(id);
        return user == null ? Result.error("用户不存在") : Result.success(toVo(user));
    }

    @GetMapping("/{id}/activities")
    public Result<Map<String, Object>> activities(@PathVariable Integer id) {
        if (userMapper.selectById(id) == null) return Result.error("用户不存在");
        var timeline = activityMapper.selectList(new LambdaQueryWrapper<UserActivity>()
                .eq(UserActivity::getUserId, id).orderByDesc(UserActivity::getCreateTime).last("LIMIT 100"));
        return Result.success(Map.of("timeline", timeline, "interests", activityMapper.selectInterests(id)));
    }

    @GetMapping("/{id}/details")
    public Result<Map<String, Object>> details(@PathVariable Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");

        var favorites = favoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, id).orderByDesc(UserFavorite::getCreateTime).last("LIMIT 200"));
        var leads = leadMapper.selectList(new LambdaQueryWrapper<CustomerLead>()
                .eq(CustomerLead::getUserId, id).orderByDesc(CustomerLead::getCreateTime).last("LIMIT 200"));
        var reactions = reactionMapper.selectList(new LambdaQueryWrapper<SchoolReaction>()
                .eq(SchoolReaction::getUserId, id).orderByDesc(SchoolReaction::getUpdatedTime).last("LIMIT 200"));
        var blessings = blessingMapper.selectList(new LambdaQueryWrapper<Blessing>()
                .eq(Blessing::getOpenid, user.getOpenid()).orderByDesc(Blessing::getCreateTime).last("LIMIT 200"));

        List<Map<String, Object>> reactionDetails = new ArrayList<>();
        for (SchoolReaction reaction : reactions) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", reaction.getId());
            row.put("schoolSource", reaction.getSchoolSource());
            row.put("schoolId", reaction.getSchoolId());
            row.put("schoolName", resolveSchoolName(reaction.getSchoolSource(), reaction.getSchoolId()));
            row.put("reactionType", reaction.getReactionType());
            row.put("reactionLabel", reactionLabel(reaction.getReactionType()));
            row.put("createdTime", reaction.getCreatedTime());
            row.put("updatedTime", reaction.getUpdatedTime());
            reactionDetails.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("favorites", favorites);
        result.put("leads", leads);
        result.put("reactions", reactionDetails);
        result.put("blessings", blessings);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer status = body == null ? null : body.get("status");
        if (status == null || (status != 0 && status != 1)) return Result.error("状态只支持正常或停用");
        User user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success();
    }

    @PutMapping("/{id}/remark")
    public Result<Void> remark(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String remark = body == null ? "" : body.getOrDefault("remark", "").trim();
        if (remark.length() > 500) return Result.error("备注最多 500 字");
        User user = userMapper.selectById(id);
        if (user == null) return Result.error("用户不存在");
        if (StringUtils.hasText(user.getPhone())) {
            userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                    .eq(User::getPhone, user.getPhone())
                    .set(User::getRemark, remark));
        } else {
            user.setRemark(remark);
            userMapper.updateById(user);
        }
        return Result.success();
    }

    private AdminUserVO toVo(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId()); vo.setNickname(user.getNickname()); vo.setAvatar(user.getAvatar());
        vo.setOpenidMasked(mask(user.getOpenid(), 4, 4)); vo.setPhone(user.getPhone());
        vo.setPhoneMasked(mask(user.getPhone(), 3, 4));
        vo.setCreateTime(user.getCreateTime()); vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastActiveTime(user.getLastActiveTime()); vo.setLoginCount(user.getLoginCount());
        vo.setStatus(user.getStatus()); vo.setRemark(user.getRemark());
        vo.setFavoriteCount(favoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>().eq(UserFavorite::getUserId, user.getId())));
        vo.setLeadCount(leadMapper.selectCount(new LambdaQueryWrapper<CustomerLead>().eq(CustomerLead::getUserId, user.getId())));
        vo.setReactionCount(reactionMapper.selectCount(new LambdaQueryWrapper<SchoolReaction>().eq(SchoolReaction::getUserId, user.getId())));
        vo.setBlessingCount(blessingMapper.selectCount(new LambdaQueryWrapper<Blessing>().eq(Blessing::getOpenid, user.getOpenid())));
        return vo;
    }

    private String mask(String value, int prefix, int suffix) {
        if (!StringUtils.hasText(value)) return "";
        if (value.length() <= prefix + suffix) return "****";
        return value.substring(0, prefix) + "****" + value.substring(value.length() - suffix);
    }

    private String reactionLabel(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "夯";
            case 2 -> "赞";
            case 3 -> "一般";
            case 4 -> "差";
            case 5 -> "拉";
            default -> "未知(" + type + ")";
        };
    }

    private String resolveSchoolName(String source, Integer id) {
        String name = switch (source == null ? "international" : source) {
            case "international" -> { School s = schoolMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "score" -> { ScoreQueryToolSchool s = scoreSchoolMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "junior" -> { JuniorHighSchool s = juniorSchoolMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "general" -> { GeneralHighSchool s = generalSchoolMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "primary" -> { PrimarySchool s = primarySchoolMapper.selectById(id); yield s == null ? null : s.getName(); }
            default -> null;
        };
        return name == null ? "学校ID " + id : name;
    }
}
