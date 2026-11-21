package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查分工具-学校风光图表 (score_query_tool_school_image)
 */
@Data
@TableName("score_query_tool_school_image")
public class ScoreQueryToolSchoolImage {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联学校ID（score_query_tool_school.id） */
    private Integer schoolId;

    /** 图片URL */
    private String imageUrl;

    /** 排序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
