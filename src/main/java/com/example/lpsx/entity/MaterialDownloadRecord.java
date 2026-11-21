package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material_download_record")
public class MaterialDownloadRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    @TableField("_openid")
    private String openid;
    private Long resourceId;
    private String sourceType;
    private Long sourceAttachmentId;
    private String resourceCode;
    private String resourceTitle;
    private String fileName;
    private String fileUrl;
    private LocalDateTime downloadTime;
}
