package com.example.lpsx.vo;

import lombok.Data;

@Data
public class AdmissionContentArticleListVO {
    private Long id;
    private String category;
    private String title;
    private Integer year;
    private String contentFormat;
    private String coverImageUrl;
    private Integer imageCount;
    private Integer sortOrder;
}
