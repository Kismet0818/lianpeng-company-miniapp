package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("zhongkao_resource_file")
public class ZhongkaoResourceFile {
    @TableId(type = IdType.AUTO) private Long id;
    private String resourceCode;
    private String title;
    private String resourceCategory;
    private String subjectCategory;
    private String resourceType;
    private String targetYear;
    private String sourceYear;
    private String originalFileName;
    private String relativePath;
    private String fileExtension;
    private String mimeType;
    private Long fileSize;
    private String fileSha256;
    @TableField(select = false) private byte[] fileContent;
    private String ossUrl;
    private String sourceBatch;
    private Integer sortOrder;
    private Integer status;
    private Integer isDeleted;
}
