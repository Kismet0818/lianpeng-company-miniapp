package com.example.lpsx.vo;

import lombok.Data;

@Data
public class SchoolMaterialAttachmentVO {
    private Long id;
    private String name;
    private String url;
    private String extension;
    private String mimeType;
    private Long size;
    private Integer sort;
}
