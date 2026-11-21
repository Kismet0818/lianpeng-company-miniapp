package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("school_reaction")
public class SchoolReaction {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String schoolSource;
    private Integer schoolId;
    private Integer userId;
    private Integer reactionType;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
