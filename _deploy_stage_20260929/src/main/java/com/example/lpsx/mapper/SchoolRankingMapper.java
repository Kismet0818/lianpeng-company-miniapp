package com.example.lpsx.mapper;

import com.example.lpsx.vo.HurunRankingRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SchoolRankingMapper {
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM school_ranking_entry r
            WHERE r.ranking_code LIKE 'hurun-%'
              AND r.ranking_year = #{year}
              AND r.status = 1 AND r.is_deleted = 0
              <if test='city != null and city != ""'>AND r.city = #{city}</if>
            </script>
            """)
    long countHurun(@Param("year") int year, @Param("city") String city);

    @Select("""
            <script>
            SELECT r.rank_position AS `rank`, r.rank_index AS `index`,
                   r.display_name AS name, r.city AS city,
                   CASE WHEN s.id IS NOT NULL AND s.is_active = 1 THEN r.school_id ELSE NULL END AS schoolId,
                   r.school_source AS schoolSource
            FROM school_ranking_entry r
            LEFT JOIN school s ON s.id = r.school_id
            WHERE r.ranking_code LIKE 'hurun-%'
              AND r.ranking_year = #{year}
              AND r.status = 1 AND r.is_deleted = 0
              <if test='city != null and city != ""'>AND r.city = #{city}</if>
            ORDER BY r.rank_position ASC, r.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<HurunRankingRecordVO> selectHurun(@Param("year") int year, @Param("city") String city,
                                           @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            SELECT ranking_name
            FROM school_ranking_entry
            WHERE ranking_code LIKE 'hurun-%' AND ranking_year=#{year}
              AND status=1 AND is_deleted=0
            ORDER BY id LIMIT 1
            """)
    String selectTitle(@Param("year") int year);
}
