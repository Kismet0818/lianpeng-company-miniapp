package com.example.lpsx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.SchoolReaction;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface SchoolReactionMapper extends BaseMapper<SchoolReaction> {
    @Select("SELECT reaction_type AS reactionType, COUNT(*) AS reactionCount " +
            "FROM school_reaction WHERE school_source=#{schoolSource} AND school_id=#{schoolId} GROUP BY reaction_type")
    List<Map<String, Object>> selectCounts(String schoolSource, Integer schoolId);

    @Select("SELECT reaction_type FROM school_reaction WHERE school_source=#{schoolSource} AND school_id=#{schoolId} AND user_id=#{userId}")
    Integer selectMine(String schoolSource, Integer schoolId, Integer userId);

    /** 按 (school_source, school_id) 统计评价数，用于首页热门排序 */
    @Select("SELECT school_source AS schoolSource, school_id AS schoolId, COUNT(*) AS cnt " +
            "FROM school_reaction GROUP BY school_source, school_id")
    List<Map<String, Object>> selectReactionCountsBySchool();

    @Insert("INSERT INTO school_reaction (school_source,school_id,user_id,reaction_type) VALUES (#{schoolSource},#{schoolId},#{userId},#{reactionType}) " +
            "ON DUPLICATE KEY UPDATE reaction_type=VALUES(reaction_type),updated_time=CURRENT_TIMESTAMP")
    int upsert(String schoolSource, Integer schoolId, Integer userId, Integer reactionType);
}
