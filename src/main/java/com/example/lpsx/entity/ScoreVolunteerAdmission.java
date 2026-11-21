package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 高中统一招生 1-15 志愿录取数据。 */
@Data
@TableName("score_volunteer_admission")
public class ScoreVolunteerAdmission {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer highSchoolId;
    /** score=查分高中库；general=一般高中库。 */
    private String source;
    private String admissionScope;
    private String targetDistrict;
    private Integer admissionYear;
    private Integer planCount;
    private BigDecimal minScore;
    private BigDecimal changeRange;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
