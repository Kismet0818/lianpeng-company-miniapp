package com.example.lpsx.dto;

import lombok.Data;

@Data
public class JuniorHighPrimaryDuikouDTO {
    private Integer admissionYear;
    private String district;
    private Integer juniorHighSchoolId;
    private String juniorHighSchoolName;
    private Integer primarySchoolId;
    private String primarySchoolName;
    private String relationType;
    private String admissionMethod;
    private String scopeText;
    private String note;
    private Integer sortOrder;
    private Integer status;
}
