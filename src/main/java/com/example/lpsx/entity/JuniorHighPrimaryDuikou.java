package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("junior_high_primary_duikou")
public class JuniorHighPrimaryDuikou {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer duikouMaterialId;
    private Integer admissionYear;
    private String district;
    private Integer juniorHighSchoolId;
    @TableField(exist = false)
    private String juniorHighSchoolName;
    private Integer primarySchoolId;
    private String primarySchoolName;
    private String relationType;
    private String admissionMethod;
    private String scopeText;
    private String note;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
