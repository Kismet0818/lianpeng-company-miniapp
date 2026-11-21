package com.example.lpsx.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScoreSchoolSearchVO {
    private Integer id;
    private String name;
    private String shortName;
    private String district;
    private String level;
    private String schoolSource;
}
