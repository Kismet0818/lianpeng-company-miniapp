package com.example.lpsx.vo;

import com.example.lpsx.entity.JuniorHighPrimaryDuikou;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DuikouSchoolGroupVO {
    private Integer juniorHighSchoolId;
    private String juniorHighSchoolName;
    private String district;
    private List<JuniorHighPrimaryDuikou> items = new ArrayList<>();
}
