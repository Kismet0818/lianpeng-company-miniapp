package com.example.lpsx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.ScoreVolunteer;
import com.example.lpsx.entity.ScoreVolunteerAdmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 小程序 1-15 志愿公开查询：数据来源为新表 score_volunteer_admission。
 * 保持 ScoreVolunteer 返回结构，避免前端改造。
 */
@Mapper
public interface ScoreVolunteerAdmissionPublicMapper extends BaseMapper<ScoreVolunteerAdmission> {

    @Select("SELECT CONCAT(admission_year - 1, '-', admission_year) " +
            "FROM score_volunteer_admission GROUP BY admission_year ORDER BY admission_year DESC")
    List<String> selectAcademicYears();

    @Select("SELECT DISTINCT target_district FROM score_volunteer_admission " +
            "WHERE target_district IS NOT NULL AND target_district != '' ORDER BY target_district")
    List<String> selectTargetDistricts();

    @Select("SELECT DISTINCT COALESCE(s.school_type, g.school_type) " +
            "FROM score_volunteer_admission a " +
            "LEFT JOIN score_query_tool_school s ON a.source = 'score' AND a.high_school_id = s.id " +
            "LEFT JOIN general_high_school g ON a.source = 'general' AND a.high_school_id = g.id " +
            "WHERE COALESCE(s.school_type, g.school_type) IS NOT NULL " +
            "AND COALESCE(s.school_type, g.school_type) != '' " +
            "ORDER BY COALESCE(s.school_type, g.school_type)")
    List<String> selectSchoolTypes();

    @Select("<script>" +
            "SELECT a.id, a.high_school_id, " +
            "COALESCE(s.name, g.name) AS school_name, " +
            "COALESCE(s.district, g.district) AS high_school_district, " +
            "COALESCE(s.school_type, g.school_type) AS school_type, " +
            "a.target_district, a.plan_count, a.min_score, " +
            "1 AS module_type, a.change_range, " +
            "CONCAT(a.admission_year - 1, '-', a.admission_year) AS academic_year, " +
            "a.created_time, a.updated_time " +
            "FROM score_volunteer_admission a " +
            "LEFT JOIN score_query_tool_school s ON a.source = 'score' AND a.high_school_id = s.id " +
            "LEFT JOIN general_high_school g ON a.source = 'general' AND a.high_school_id = g.id " +
            "WHERE a.admission_year = #{admissionYear} " +
            "<if test='district != null and district != \"\"'>AND a.target_district = #{district} </if>" +
            "<if test='type != null and type != \"\"'>AND COALESCE(s.school_type, g.school_type) = #{type} </if>" +
            "<if test='highSchoolId != null'>AND a.source = 'score' AND a.high_school_id = #{highSchoolId} </if>" +
            "ORDER BY a.target_district, a.min_score DESC, a.id" +
            "</script>")
    List<ScoreVolunteer> selectVolunteerList(@Param("admissionYear") Integer admissionYear,
                                              @Param("district") String district,
                                              @Param("type") String type,
                                              @Param("highSchoolId") Integer highSchoolId);
}
