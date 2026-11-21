package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 查分工具-1-15志愿（统一招生）录取分数线表 (score_volunteer_1_15)
 */
@Data
@TableName("score_volunteer_1_15")
public class ScoreVolunteer {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联的查分工具高中 ID；历史简称无法可靠映射时为 null */
    @TableField(exist = false)
    private Integer highSchoolId;

    /** 高中学校名称 */
    private String schoolName;

    /** 高中学校所属区 */
    private String highSchoolDistrict;

    /** 学校级别（市/区实验性示范性高中、市特色普通高中、一般高中） */
    private String schoolType;

    /** 招生目标区（考生所在区） */
    private String targetDistrict;

    /** 计划招生数（1-15志愿批次） */
    private Integer planCount;

    /** 最低录取分数线 */
    private BigDecimal minScore;

    /** 模块类型：1-统一招生（1-15志愿），2-自主招生 */
    private Integer moduleType;

    /** 自主招生-学科优秀录取线 */
    private BigDecimal subjectExcellent;

    /** 自主招生-体育优秀录取线 */
    private BigDecimal sportsExcellent;

    /** 自主招生-艺术骨干录取线 */
    private BigDecimal artBackbone;

    /** 涨幅（正数上涨，负数下降） */
    private BigDecimal changeRange;

    /** 学年（如 2025-2026） */
    private String academicYear;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
