package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("junior_high_school")
public class JuniorHighSchool {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String shortName;
    private String backgroundImageUrl;
    private String district;
    /** 最低学费（万元/年） */
    private BigDecimal tuitionMin;
    /** 最高学费（万元/年） */
    private BigDecimal tuitionMax;
    private String boardingInfo;
    private String schoolType;
    private String tier;
    private String address;
    private String description;
    private String officialWebsite;
    private String schedule;
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
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
