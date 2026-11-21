package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("platform_content_link")
public class PlatformContentLink {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String contentType;
    private String title;
    private String linkUrl;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
