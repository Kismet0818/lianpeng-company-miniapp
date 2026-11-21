package com.example.lpsx.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.ScoreVolunteer;

/**
 * 查分工具-1-15志愿 Mapper
 */
@Mapper
public interface ScoreVolunteerMapper extends BaseMapper<ScoreVolunteer> {

    /** 全部学年（去重，倒序） */
    @Select("SELECT DISTINCT academic_year FROM score_volunteer_1_15 ORDER BY academic_year DESC")
    List<String> selectYears();

    /** 可选招生目标区（去重） */
    @Select("SELECT DISTINCT target_district FROM score_volunteer_1_15 " +
            "WHERE target_district IS NOT NULL AND target_district != '' ORDER BY target_district")
    List<String> selectTargetDistricts();

    /** 可选学校级别（去重） */
    @Select("SELECT DISTINCT school_type FROM score_volunteer_1_15 " +
            "WHERE school_type IS NOT NULL AND school_type != '' ORDER BY school_type")
    List<String> selectSchoolTypes();

    /**
     * 查询 1-15 志愿，并通过学校全名或唯一简称补充高中 ID。
     */
    @Select("<script>" +
            "SELECT v.id, v.module_type, v.school_name, v.high_school_district, v.school_type, " +
            "v.target_district, v.plan_count, v.min_score, v.academic_year, v.created_time, " +
            "v.updated_time, v.change_range, v.subject_excellent, v.sports_excellent, " +
            "v.art_backbone, s.id AS high_school_id " +
            "FROM (" +
            "SELECT base.*, ROW_NUMBER() OVER (PARTITION BY base.module_type, base.school_name, " +
            "COALESCE(base.high_school_district, ''), COALESCE(base.school_type, ''), " +
            "base.target_district, base.plan_count, COALESCE(base.min_score, -1), " +
            "base.academic_year, COALESCE(base.change_range, -9999) ORDER BY base.id) AS rn " +
            "FROM score_volunteer_1_15 base " +
            "WHERE base.module_type = 1 AND base.academic_year = #{year} " +
            "<if test='district != null and district != \"\"'>AND base.target_district = #{district} </if>" +
            "<if test='type != null and type != \"\"'>AND base.school_type = #{type} </if>" +
            ") v " +
            "LEFT JOIN score_query_tool_school s " +
            "ON (v.school_name = s.name OR v.school_name = s.short_name) " +
            "WHERE v.rn = 1 " +
            "<if test='highSchoolId != null'>AND s.id = #{highSchoolId} </if>" +
            "ORDER BY v.school_name, v.target_district, v.min_score DESC, v.id" +
            "</script>")
    List<ScoreVolunteer> selectVolunteerList(@Param("year") String year,
                                              @Param("district") String district,
                                              @Param("type") String type,
                                              @Param("highSchoolId") Integer highSchoolId);
}
