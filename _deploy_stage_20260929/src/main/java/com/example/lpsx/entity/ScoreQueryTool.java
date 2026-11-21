package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 查分工具-名额分配表 (score_query_tool)
 * target_type: 1-分配到校, 2-分配到区
 */
@Data
@TableName("score_query_tool")
public class ScoreQueryTool {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 高中学校 ID（关联 score_query_tool_school） */
    private Integer highSchoolId;

    /** 高中学校名称 */
    private String highSchoolName;

    /** 高中学校所属区 */
    private String highSchoolDistrict;

    /** 目标类型：1-分配到校，2-分配到区 */
    private Integer targetType;

    /** 目标区（分配到区时必填） */
    private String targetDistrict;

    /** 目标初中学校名称（分配到校时必填） */
    private String targetSchoolName;

    /** 名额数量 */
    private Integer quotaCount;

    /** 最低录取分数线 */
    private Integer minScore;

    /** 涨幅（正数上涨，负数下降） */
    private BigDecimal changeRange;

    /** 学年（如 2025-2026） */
    private String academicYear;
}
