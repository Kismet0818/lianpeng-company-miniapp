package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 民办小初招生摇号数据表 (school_enrollment)
 * 通过 school_name 与学校表关联，不使用 id。
 */
@Data
@TableName("school_enrollment")
public class SchoolEnrollment {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学校名称（与 primary_school.name / school.name 一致） */
    private String schoolName;

    /** 行政区 */
    private String district;

    /** 学段：小学 / 初中 */
    private String schoolStage;

    /** 办学性质：民办 */
    private String ownershipType;

    /** 校区/类型：走读、住宿、北校区走读等 */
    private String campus;

    /** 招生年份 */
    private Integer enrollYear;

    /** 计划招生人数 */
    private Integer enrollmentCount;

    /** 摇中率（0~1 小数） */
    private BigDecimal lotteryRate;

    /** 中考最高分（仅初中） */
    private BigDecimal maxScore;

    /** 700 分以上人数（可为文本，仅初中） */
    @TableField("score_700_count")
    private String score700Count;

    /** 市重点率（0~1 小数，仅初中） */
    private BigDecimal keyHighRate;

    /** 备注（原文说明等） */
    private String remark;
}
