package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("general_high_school")
public class GeneralHighSchool {
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
    private String address;
    private String description;
    private String officialWebsite;
    private String schedule;
    private String teachingFeatures;
    /** 占地面积（平方米） */
    private BigDecimal campusArea;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
