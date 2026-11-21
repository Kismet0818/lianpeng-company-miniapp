package com.example.lpsx.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchoolCompareDetailsVO {
    private String schoolSystem;
    private String level;
    private List<SchoolCompareVO> schools;
}
