package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("admission_content_article")
public class AdmissionContentArticle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceCategory;
    private String title;
    private Integer articleYear;
    private String contentFormat;
    private String contentText;
    private String ossImageUrls;
    private Integer imageCount;
    private String captureStatus;
    private Integer sortOrder;
    private Integer status;
}
