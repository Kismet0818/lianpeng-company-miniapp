package com.example.lpsx.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ScoreVolunteerAdmissionDTO {
    private Integer highSchoolId;
    private String source;
    private String admissionScope;
    private String targetDistrict;
    private Integer admissionYear;
    private Integer planCount;
    private BigDecimal minScore;
    private BigDecimal changeRange;
}
