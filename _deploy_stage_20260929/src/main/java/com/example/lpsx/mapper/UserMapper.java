package com.example.lpsx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT
              COUNT(CASE WHEN first_seen >= CURDATE() THEN 1 END) AS todayVisitors,
              COUNT(CASE WHEN first_seen >= CURDATE() AND authorized = 1 THEN 1 END) AS todayUsers,
              COUNT(CASE WHEN first_seen >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND first_seen < CURDATE() THEN 1 END) AS yesterdayVisitors,
              COUNT(CASE WHEN first_seen >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND first_seen < CURDATE() AND authorized = 1 THEN 1 END) AS yesterdayUsers,
              COUNT(CASE WHEN first_seen >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY) THEN 1 END) AS weekVisitors,
              COUNT(CASE WHEN first_seen >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY) AND authorized = 1 THEN 1 END) AS weekUsers,
              COUNT(*) AS totalVisitors,
              COUNT(CASE WHEN authorized = 1 THEN 1 END) AS totalUsers
            FROM (
              SELECT visitor_key, MAX(authorized) AS authorized, MIN(create_time) AS first_seen
              FROM (
                SELECT CASE WHEN phone IS NOT NULL AND phone <> '' THEN CONCAT('phone:', phone)
                            ELSE CONCAT('openid:', openid) END AS visitor_key,
                       CASE WHEN phone IS NOT NULL AND phone <> '' THEN 1 ELSE 0 END AS authorized,
                       create_time
                FROM `user`
              ) identities
              GROUP BY visitor_key
            ) users_by_identity
            """)
    Map<String, Object> selectUsageStats();
}
