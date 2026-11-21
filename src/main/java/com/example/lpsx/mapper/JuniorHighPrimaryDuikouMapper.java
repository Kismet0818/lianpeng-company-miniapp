package com.example.lpsx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.JuniorHighPrimaryDuikou;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface JuniorHighPrimaryDuikouMapper extends BaseMapper<JuniorHighPrimaryDuikou> {
    @Select("""
        <script>
        SELECT COUNT(DISTINCT d.junior_high_school_id)
        FROM junior_high_primary_duikou d
        JOIN junior_high_school j ON j.id = d.junior_high_school_id
        WHERE 1=1
        <if test='admissionYear != null'>AND d.admission_year = #{admissionYear}</if>
        <if test='district != null and district != ""'>AND d.district = #{district}</if>
        <if test='juniorHighSchoolName != null and juniorHighSchoolName != ""'>AND j.name LIKE CONCAT('%', #{juniorHighSchoolName}, '%')</if>
        <if test='keyword != null and keyword != ""'>AND (d.primary_school_name LIKE CONCAT('%', #{keyword}, '%') OR d.scope_text LIKE CONCAT('%', #{keyword}, '%'))</if>
        </script>
        """)
    long countAdminGroups(@Param("admissionYear") Integer admissionYear,
                          @Param("district") String district,
                          @Param("juniorHighSchoolName") String juniorHighSchoolName,
                          @Param("keyword") String keyword);

    @Select("""
        <script>
        SELECT d.junior_high_school_id
        FROM junior_high_primary_duikou d
        JOIN junior_high_school j ON j.id = d.junior_high_school_id
        WHERE 1=1
        <if test='admissionYear != null'>AND d.admission_year = #{admissionYear}</if>
        <if test='district != null and district != ""'>AND d.district = #{district}</if>
        <if test='juniorHighSchoolName != null and juniorHighSchoolName != ""'>AND j.name LIKE CONCAT('%', #{juniorHighSchoolName}, '%')</if>
        <if test='keyword != null and keyword != ""'>AND (d.primary_school_name LIKE CONCAT('%', #{keyword}, '%') OR d.scope_text LIKE CONCAT('%', #{keyword}, '%'))</if>
        GROUP BY d.junior_high_school_id, j.name
        ORDER BY j.name ASC, d.junior_high_school_id ASC
        LIMIT #{offset}, #{size}
        </script>
        """)
    List<Integer> selectAdminGroupSchoolIds(@Param("admissionYear") Integer admissionYear,
                                            @Param("district") String district,
                                            @Param("juniorHighSchoolName") String juniorHighSchoolName,
                                            @Param("keyword") String keyword,
                                            @Param("offset") long offset,
                                            @Param("size") long size);

    @Select("""
        SELECT d.id, d.admission_year AS admissionYear, d.district,
               NULL AS primarySchoolId,
               d.primary_school_name AS primarySchoolName,
               NULL AS primarySchoolShortName,
               NULL AS primarySchoolAddress,
               NULL AS primarySchoolImage,
               d.relation_type AS relationType,
               d.admission_method AS admissionMethod,
               d.scope_text AS scopeText,
               d.note,
               d.duikou_material_id AS materialId,
               m.title AS materialTitle
        FROM junior_high_primary_duikou d
        LEFT JOIN duikou_material m ON m.id = d.duikou_material_id
        WHERE d.junior_high_school_id = #{juniorHighSchoolId}
          AND d.admission_year = #{admissionYear}
          AND d.status = 1
        ORDER BY d.sort_order ASC, primarySchoolName ASC, d.id ASC
        """)
    List<Map<String, Object>> selectDetail(Integer juniorHighSchoolId, Integer admissionYear);
}
