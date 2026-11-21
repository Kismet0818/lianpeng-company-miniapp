package com.example.lpsx.vo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchoolCompareVO {
    private Integer id;
    private String compareKey;
    private String schoolSource;
    private String kind;
    private String name;
    private String shortName;
    private String district;
    private String level;
    private String schoolType;
    private String nature;
    private BigDecimal tuitionMin;
    private BigDecimal tuitionMax;
    private String boardingInfo;
    private String logoUrl;
    private String address;
    private String description;
    private String officialWebsite;
    private String schedule;
    private String teachingFeatures;
    private BigDecimal campusArea;
}
