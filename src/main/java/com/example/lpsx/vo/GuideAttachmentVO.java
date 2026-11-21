package com.example.lpsx.vo;

import lombok.Data;

/** 政策文章附件，供小程序以按钮形式下载和预览。 */
@Data
public class GuideAttachmentVO {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String fileExtension;
    private String mediaType;
    private Integer sortOrder;
}
