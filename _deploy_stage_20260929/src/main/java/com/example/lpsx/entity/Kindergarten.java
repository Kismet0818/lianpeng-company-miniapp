package com.example.lpsx.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("kindergarten")
public class Kindergarten {
    @TableId(type = IdType.AUTO) private Integer id;
    private String schoolCode;
    private Long sourceSchoolId;
    private String name;
    private String shortName;
    private String backgroundImageUrl;
    private String district;
    private BigDecimal tuitionMin;
    private BigDecimal tuitionMax;
    private String schoolType;
    private String kindergartenLevel;
    private String careType;
    private String ageRange;
    private String address;
    private String phone;
    private String description;
    private String officialWebsite;
    private String schedule;
    private String teachingFeatures;
    private BigDecimal campusArea;
    private Boolean isActive;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    @TableField(exist = false) private final String schoolSource = "kindergarten";
    @TableField(exist = false) private final String schoolLevel = "幼儿园";
}
