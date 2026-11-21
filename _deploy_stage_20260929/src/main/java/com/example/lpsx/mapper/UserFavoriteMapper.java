package com.example.lpsx.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.UserFavorite;
import com.example.lpsx.vo.FavoriteVO;

/**
 * 用户收藏 Mapper
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 查询收藏列表 — 三表 JOIN 一次返回所有信息
     */
    @Select("SELECT f.id AS favoriteId, u.id AS userId, u.nickname AS userNickname, " +
            "u.avatar AS userAvatar, f.school_id AS schoolId, f.school_source AS schoolSource, " +
            "COALESCE(s.name, qs.name, j.name, g.name, p.name, f.school_name) AS schoolName, " +
            "COALESCE(s.logo_url, qs.logo, j.background_image_url, g.background_image_url, p.background_image_url) AS schoolLogo, " +
            "COALESCE(s.district, qs.district, j.district, g.district, p.district) AS district, " +
            "s.curriculum, s.grades, " +
            "f.create_time AS createTime " +
            "FROM user_favorite f " +
            "JOIN user u ON f.user_id = u.id " +
            "LEFT JOIN school s ON f.school_source = 'international' AND f.school_id = s.id " +
            "LEFT JOIN score_query_tool_school qs ON f.school_source = 'score' AND f.school_id = qs.id " +
            "LEFT JOIN junior_high_school j ON f.school_source = 'junior' AND f.school_id = j.id " +
            "LEFT JOIN general_high_school g ON f.school_source = 'general' AND f.school_id = g.id " +
            "LEFT JOIN primary_school p ON f.school_source = 'primary' AND f.school_id = p.id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC")
    List<FavoriteVO> selectFavoriteList(@Param("userId") Integer userId);
}
