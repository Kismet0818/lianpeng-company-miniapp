package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("primary_school")
public class PrimarySchool {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String schoolCode;
    private Integer sourceSchoolId;
    private String name;
    private String shortName;
    private String backgroundImageUrl;
    private String district;
    private BigDecimal tuitionMin;
    private BigDecimal tuitionMax;
    private String boardingInfo;
    private String schoolType;
    private String tier;
    private String address;
    private String description;
    private String officialWebsite;
    private String schedule;
    private String teachingFeatures;
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

    @TableField(exist = false)
    private final String schoolSource = "primary";

    @TableField(exist = false)
    private final String schoolLevel = "小学";
}
