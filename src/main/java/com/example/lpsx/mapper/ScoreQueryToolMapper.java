package com.example.lpsx.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.ScoreQueryTool;

/**
 * 查分工具-名额分配 Mapper
 */
@Mapper
public interface ScoreQueryToolMapper extends BaseMapper<ScoreQueryTool> {

    /** 全部学年（去重，倒序） */
    @Select("SELECT DISTINCT academic_year FROM score_query_tool ORDER BY academic_year DESC")
    List<String> selectYears();

    /** 可选目标区（去重） */
    @Select("SELECT DISTINCT target_district FROM score_query_tool " +
            "WHERE target_district IS NOT NULL AND target_district != '' ORDER BY target_district")
    List<String> selectTargetDistricts();

    /** 可选目标初中（去重，仅分配到校记录） */
    @Select("SELECT DISTINCT target_school_name FROM score_query_tool " +
            "WHERE target_type = 1 AND target_school_name IS NOT NULL AND target_school_name != '' " +
            "ORDER BY target_school_name")
    List<String> selectJuniorSchools();
}
