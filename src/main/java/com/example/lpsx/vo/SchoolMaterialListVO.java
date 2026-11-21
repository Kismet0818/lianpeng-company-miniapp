package com.example.lpsx.vo;

import lombok.Data;

@Data
public class SchoolMaterialListVO {
    private Long id;
    private String resourceCode;
    private String title;
    private String resourceCategory;
    private String subjectCategory;
    private String resourceType;
    private String summary;
    private String publishDate;
    private String year;
    private String coverUrl;
    private boolean hasAttachment;
}
