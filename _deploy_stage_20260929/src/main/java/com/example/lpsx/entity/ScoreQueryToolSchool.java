package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 查分工具-学校信息表 (score_query_tool_school)
 */
@Data
@TableName("score_query_tool_school")
public class ScoreQueryToolSchool {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学校名称 */
    private String name;

    /** 学校名称缩写 */
    private String shortName;

    /** 学校Logo URL */
    private String logo;

    /** 所属区 */
    private String district;

    /** 最低学费（万元/年） */
    private BigDecimal tuitionMin;

    /** 最高学费（万元/年） */
    private BigDecimal tuitionMax;

    /** 寄宿情况 */
    private String boardingInfo;

    /** 学校性质 */
    private String schoolType;

    /** 梯队 */
    private String tier;

    /** 学校地址 */
    private String address;

    /** 学校介绍 */
    private String description;

    /** 学校官网 */
    private String officialWebsite;

    /** 作息时间 */
    private String schedule;

    /** 教学特色 */
    private String teachingFeatures;

    /** 占地面积（平方米） */
    private BigDecimal campusArea;
    private String foundedYear;
    private String phone;
    private String feesStandard;
    private String facultyStrength;
    private String featuresCourses;
    private String foreignSituation;
    private String competitionResult;
    private Integer enrollmentYear;
    private String enrollmentTarget;
    private String enrollmentPlanning;
    private String enrollmentWay;
    private String isBus;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime sourceUpdateTime;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
