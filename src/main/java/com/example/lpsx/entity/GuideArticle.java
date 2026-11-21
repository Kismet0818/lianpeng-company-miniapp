package com.example.lpsx.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
@Data
@TableName("guide_article")
public class GuideArticle {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String category;
    /** 升学体系：external=体制外，internal=体制内 */
    private String educationSystem;
    private String pathName;
    private String title;
    private String content;
    private String summary;
    private String thumbnailUrl;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
}
