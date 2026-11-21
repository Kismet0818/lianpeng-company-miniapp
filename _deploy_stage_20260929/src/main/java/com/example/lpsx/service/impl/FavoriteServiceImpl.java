package com.example.lpsx.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.School;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.entity.GeneralHighSchool;
import com.example.lpsx.entity.PrimarySchool;
import com.example.lpsx.entity.Kindergarten;
import com.example.lpsx.entity.User;
import com.example.lpsx.entity.UserFavorite;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.GeneralHighSchoolMapper;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import com.example.lpsx.mapper.KindergartenMapper;
import com.example.lpsx.mapper.UserFavoriteMapper;
import com.example.lpsx.mapper.UserMapper;
import com.example.lpsx.service.FavoriteService;
import com.example.lpsx.vo.FavoriteVO;

import jakarta.annotation.Resource;

/**
 * 收藏服务实现
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final java.util.Set<String> SOURCES =
            java.util.Set.of("international", "score", "junior", "general", "primary", "kindergarten");

    @Resource
    private UserFavoriteMapper favoriteMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SchoolMapper schoolMapper;

    @Resource private ScoreQueryToolSchoolMapper scoreMapper;
    @Resource private JuniorHighSchoolMapper juniorMapper;
    @Resource private GeneralHighSchoolMapper generalMapper;
    @Resource private PrimarySchoolMapper primaryMapper;
    @Resource private KindergartenMapper kindergartenMapper;

    @Override
    public void toggle(Integer userId, String schoolSource, Integer schoolId) {
        if (userId == null || schoolId == null || schoolId <= 0) {
            throw new BusinessException("学校编号不正确");
        }
        String source = normalizeSource(schoolSource);
        if (source == null) {
            throw new BusinessException("学校来源不正确");
        }
        String schoolName = resolveName(source, schoolId);
        if (schoolName == null) {
            throw new BusinessException("学校不存在");
        }
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
               .eq(UserFavorite::getSchoolSource, source)
               .eq(UserFavorite::getSchoolId, schoolId);
        UserFavorite exist = favoriteMapper.selectOne(wrapper);

        if (exist != null) {
            // 已收藏 → 取消
            favoriteMapper.deleteById(exist.getId());
        } else {
            // 未收藏 → 收藏（写入时快照用户昵称和学校名称）
            User user = userMapper.selectById(userId);
            if (user == null || user.getOpenid() == null || user.getOpenid().isBlank()) {
                throw new BusinessException("用户登录信息不完整，请重新登录");
            }
            UserFavorite fav = new UserFavorite();
            fav.setUserId(userId);
            fav.setSchoolId(schoolId);
            fav.setSchoolSource(source);
            fav.setUserNickname(user.getNickname());
            fav.setSchoolName(schoolName);
            fav.setCloudOpenid(user.getOpenid());
            favoriteMapper.insert(fav);
        }
    }

    @Override
    public List<FavoriteVO> list(Integer userId) {
        return favoriteMapper.selectFavoriteList(userId);
    }

    private String normalizeSource(String source) {
        if (source == null) return null;
        String value = source.trim().toLowerCase();
        return SOURCES.contains(value) ? value : null;
    }

    private String resolveName(String source, Integer id) {
        return switch (source) {
            case "international" -> { School s = schoolMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "score" -> { ScoreQueryToolSchool s = scoreMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "junior" -> { JuniorHighSchool s = juniorMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "general" -> { GeneralHighSchool s = generalMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "primary" -> { PrimarySchool s = primaryMapper.selectById(id); yield s == null ? null : s.getName(); }
            case "kindergarten" -> { Kindergarten s = kindergartenMapper.selectById(id); yield s == null ? null : s.getName(); }
            default -> null;
        };
    }
}
