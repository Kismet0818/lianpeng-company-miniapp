package com.example.lpsx.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 学校图片关联表 (school_image)
 */
@Data
@TableName("school_image")
public class SchoolImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer schoolId;

    private String imageUrl;

    private Integer imageType;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
