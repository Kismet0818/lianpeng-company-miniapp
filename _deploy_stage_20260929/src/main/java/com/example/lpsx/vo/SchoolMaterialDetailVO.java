package com.example.lpsx.vo;

import lombok.Data;

import java.util.List;

@Data
public class SchoolMaterialDetailVO {
    private Long id;
    private String resourceCode;
    private String title;
    private String resourceCategory;
    private String subjectCategory;
    private String resourceType;
    private String year;
    private String summary;
    private String content;
    private List<SchoolMaterialAttachmentVO> attachments;
}
