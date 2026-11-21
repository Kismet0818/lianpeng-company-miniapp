package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("parent_qa")
public class ParentQa {
    @TableId private Long id;
    private String articleId;
    private String title;
    private String summary;
    private String picUrl;
    private Integer status;
    private Integer ranked;
    private String content;
    private String classify;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
