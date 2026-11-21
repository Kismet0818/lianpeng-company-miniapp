package com.example.lpsx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lpsx.entity.MaterialDownloadRecord;
import com.example.lpsx.vo.MaterialDownloadRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MaterialDownloadRecordMapper extends BaseMapper<MaterialDownloadRecord> {
    @Select("""
            <script>
            SELECT d.id, d.user_id AS userId, COALESCE(u.nickname, '微信用户') AS userNickname,
                   u.phone AS userPhone, d.resource_id AS resourceId, d.source_type AS sourceType,
                   d.source_attachment_id AS sourceAttachmentId, d.resource_code AS resourceCode,
                   d.resource_title AS resourceTitle, d.file_name AS fileName,
                   d.file_url AS fileUrl,
                   d.download_time AS downloadTime
            FROM material_download_record d
            LEFT JOIN `user` u ON u.id = d.user_id
            <where>
              <if test="keyword != null and keyword != ''">
                (d.resource_title LIKE CONCAT('%', #{keyword}, '%')
                 OR d.resource_code LIKE CONCAT('%', #{keyword}, '%')
                 OR d.file_name LIKE CONCAT('%', #{keyword}, '%')
                 OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                 OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                 OR CAST(d.user_id AS CHAR) = #{keyword})
              </if>
            </where>
            ORDER BY d.download_time DESC, d.id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<MaterialDownloadRecordVO> selectAdminPage(@Param("keyword") String keyword,
                                                    @Param("offset") long offset,
                                                    @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM material_download_record d
            LEFT JOIN `user` u ON u.id = d.user_id
            <where>
              <if test="keyword != null and keyword != ''">
                (d.resource_title LIKE CONCAT('%', #{keyword}, '%')
                 OR d.resource_code LIKE CONCAT('%', #{keyword}, '%')
                 OR d.file_name LIKE CONCAT('%', #{keyword}, '%')
                 OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                 OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                 OR CAST(d.user_id AS CHAR) = #{keyword})
              </if>
            </where>
            </script>
            """)
    long countAdmin(@Param("keyword") String keyword);
}
