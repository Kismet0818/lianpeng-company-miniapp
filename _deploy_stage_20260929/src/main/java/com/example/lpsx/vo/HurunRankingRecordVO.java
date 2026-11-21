package com.example.lpsx.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HurunRankingRecordVO {
    private Integer rank;
    private BigDecimal index;
    private String name;
    private String city;
    private Integer schoolId;
    private String schoolSource;
}
