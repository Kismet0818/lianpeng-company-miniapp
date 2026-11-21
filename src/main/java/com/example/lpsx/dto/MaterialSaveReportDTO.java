package com.example.lpsx.dto;

import lombok.Data;

@Data
public class MaterialSaveReportDTO {
    private String sourceType;
    private Integer sourceId;
    private Long attachmentId;
    private String fileUrl;
    private String fileName;
}
