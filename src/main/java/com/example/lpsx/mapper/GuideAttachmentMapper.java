package com.example.lpsx.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.lpsx.vo.GuideAttachmentVO;

@Mapper
public interface GuideAttachmentMapper {

    /**
     * 政策文章是由 education_policy 导入 guide_article 的历史结构，当前通过
     * “来源平台 + 政策标题”关联；只返回已经迁移到自有 OSS 的有效附件。
     */
    @Select("""
            SELECT DISTINCT
                   a.id AS id,
                   a.file_name AS fileName,
                   a.file_url AS fileUrl,
                   a.file_extension AS fileExtension,
                   a.media_type AS mediaType,
                   a.sort_order AS sortOrder
            FROM guide_article g
            JOIN education_policy p
              ON p.name = g.title
             AND p.source_platform = 'anroader'
             AND p.is_deleted = 0
            JOIN education_policy_attachment a
              ON a.policy_id = p.id
             AND a.is_deleted = 0
             AND a.file_url IS NOT NULL
             AND TRIM(a.file_url) <> ''
            WHERE g.id = #{articleId}
            ORDER BY sortOrder, id
            """)
    List<GuideAttachmentVO> selectByGuideArticleId(@Param("articleId") int articleId);

    @Select("""
            SELECT a.id AS id, a.file_name AS fileName, a.file_url AS fileUrl,
                   a.file_extension AS fileExtension, a.media_type AS mediaType, a.sort_order AS sortOrder
            FROM guide_article g
            JOIN education_policy p ON p.name = g.title AND p.source_platform = 'anroader' AND p.is_deleted = 0
            JOIN education_policy_attachment a ON a.policy_id = p.id AND a.is_deleted = 0
            WHERE g.id = #{articleId} AND a.id = #{attachmentId}
            LIMIT 1
            """)
    GuideAttachmentVO selectOwnedAttachment(@Param("articleId") int articleId,
                                             @Param("attachmentId") long attachmentId);
}
