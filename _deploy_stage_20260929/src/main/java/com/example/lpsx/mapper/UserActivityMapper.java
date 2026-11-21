package com.example.lpsx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.UserActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserActivityMapper extends BaseMapper<UserActivity> {
    @Select("SELECT school_source AS schoolSource, school_id AS schoolId, MAX(school_name) AS schoolName, " +
            "COUNT(*) AS viewCount, SUM(duration_seconds) AS totalDurationSeconds, MAX(create_time) AS lastViewTime " +
            "FROM user_activity WHERE user_id=#{userId} AND event_type='school_view' " +
            "GROUP BY school_source, school_id ORDER BY totalDurationSeconds DESC, viewCount DESC LIMIT 500")
    List<Map<String, Object>> selectInterests(Integer userId);

    @Select("SELECT school_source AS schoolSource, school_id AS schoolId, MAX(school_name) AS schoolName, " +
            "MAX(create_time) AS lastViewTime " +
            "FROM user_activity WHERE user_id=#{userId} AND event_type='school_view' " +
            "GROUP BY school_source, school_id ORDER BY lastViewTime DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecentSchools(@Param("userId") Integer userId,
                                                  @Param("limit") Integer limit);
}
