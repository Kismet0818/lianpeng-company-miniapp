package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 学校信息表 (school)
 */
@Data
@TableName("school")
public class School {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** API中的稳定学校来源；不对应数据库字段。 */
    @TableField(exist = false)
    private String schoolSource;

    /** 学校名称 */
    private String name;

    /** 城市 */
    private String city;

    /** 行政区 */
    private String district;

    /** 课程体系 (IB/AP/A-Level/国内) */
    private String curriculum;

    /** 最低学费（万元/年） */
    private BigDecimal tuitionMin;

    /** 最高学费（万元/年） */
    private BigDecimal tuitionMax;

    /** 专属优势标签（逗号分隔，如：双语双轨,藤校收割机） */
    private String featureTags;

    /** 开设学段 */
    private String grades;

    /** 富文本介绍 */
    private String description;

    /** 升学情况 */
    private String graduationResult;

    /** 寄宿项目 */
    private String boardingInfo;

    /** 平均班级人数 */
    private String classSize;

    /** 师生平均比例 */
    private String teacherRatio;

    /** Logo URL */
    private String logoUrl;

    /** 是否启用: 1-启用, 0-禁用 (逻辑删除) */
    @TableLogic
    private Integer isActive;

    /** 授课方式 */
    private String teachingMode;

    /** 创办时间 */
    private String foundingTime;

    /** 在校学生人数 */
    private Integer studentCount;

    /** 占地面积（平方米） */
    private BigDecimal campusArea;

    /** 中高级职称教师比例 */
    private BigDecimal teacherSeniorRatio;

    /** 学校性质 */
    private String schoolType;

    /** 学校地址 */
    private String address;

    /** 学校官网 */
    private String officialWebsite;

    /** 作息时间 */
    private String schedule;

    /** 教学特色 */
    private String teachingFeatures;

    /** 专职教师数 */
    private Integer teacherCount;

    /** 总招生人数 */
    private Integer totalRecruit;

    /** 本地生源数 */
    private Integer localStudents;

    /** 非本地生源数 */
    private Integer nonLocalStudents;
}
