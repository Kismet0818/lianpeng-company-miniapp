package com.example.lpsx.dto;

import lombok.Data;

@Data
public class SchoolFeatureClassDTO {
    private Integer schoolId;
    /** junior=初中学校库；general=高中学校库。 */
    private String source;
    private String featureClass;
    private String features;
    private String classCount;
    private String classAssignment;
}
