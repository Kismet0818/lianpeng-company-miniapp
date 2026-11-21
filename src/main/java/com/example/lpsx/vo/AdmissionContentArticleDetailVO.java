package com.example.lpsx.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdmissionContentArticleDetailVO {
    private Long id;
    private String category;
    private String title;
    private Integer year;
    private String contentFormat;
    private String contentText;
    private List<String> imageUrls = new ArrayList<>();
    private Integer imageCount;
}
