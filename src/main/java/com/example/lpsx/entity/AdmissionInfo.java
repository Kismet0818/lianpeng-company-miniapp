package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校招生动态表 (admission_info)
 */
@Data
@TableName("admission_info")
public class AdmissionInfo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联学校ID */
    private Integer schoolId;

    /** 学校名称 */
    private String schoolName;

    /** 招生年份 */
    private String admissionYear;

    /** 招生进度/活动信息 */
    private String admissionEventInfo;

    /** 招生时间 */
    private String admissionTime;

    /** 开设学段 */
    private String educationalStages;

    /** 学费区间 */
    private String tuitionRange;

    /** 招生人数 */
    private String enrollmentCount;

    /** 课程体系 */
    private String curriculum;

    /** 招生对象 */
    private String targetStudents;

    /** 录取要求 */
    private String admissionRules;

    /** 入学测试详情 */
    private String examContent;

    /** 报名与申请流程 */
    private String applicationFlow;

    /** 升学情况 */
    private String graduationResult;

    /** 是否显示 */
    private Integer isActive;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
