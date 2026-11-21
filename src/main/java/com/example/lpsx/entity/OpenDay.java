package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校开放日表 (open_day)
 */
@Data
@TableName("open_day")
public class OpenDay {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联学校ID */
    private Integer schoolId;

    /** 学校名称 */
    private String schoolName;

    /** 学段（小学/初中/高中） */
    private String educationalStages;

    /** 开设课程 */
    private String curriculum;

    /** 学校地址 */
    private String address;

    /** 开放日时间 */
    private LocalDateTime openTime;

    /** 学校介绍 */
    private String description;

    /** 是否启用: 1-启用, 0-禁用 */
    private Integer isActive;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
