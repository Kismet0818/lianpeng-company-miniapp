package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学校特色班信息。source 用于标识学校库来源：junior / general。 */
@Data
@TableName("school_feature_class")
public class SchoolFeatureClass {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer schoolId;
    private String source;
    private String district;
    private String featureClass;
    private String features;
    private String classCount;
    private String classAssignment;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
