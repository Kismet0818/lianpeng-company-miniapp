package com.example.lpsx.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaterialDownloadRecordVO {
    private Long id;
    private Integer userId;
    private String userNickname;
    private String userPhone;
    private Long resourceId;
    private String sourceType;
    private Long sourceAttachmentId;
    private String resourceCode;
    private String resourceTitle;
    private String fileName;
    private String fileUrl;
    private LocalDateTime downloadTime;
}
