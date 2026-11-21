package com.example.lpsx.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 开放日视图对象 — open_day + school.logo_url
 */
@Data
public class OpenDayVO {

    private Integer id;

    /** 关联学校ID */
    private Integer schoolId;

    /** 学校名称 */
    private String schoolName;

    /** 学段 */
    private String educationalStages;

    /** 开设课程 */
    private String curriculum;

    /** 学校地址 */
    private String address;

    /** 开放日时间 */
    private LocalDateTime openTime;

    /** 学校介绍 */
    private String description;

    /** 学校 Logo（联查 school 表） */
    private String logoUrl;
}
