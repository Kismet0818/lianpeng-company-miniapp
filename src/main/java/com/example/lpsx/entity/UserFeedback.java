package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_feedback")
public class UserFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    @TableField("_openid")
    private String cloudOpenid;
    private String category;
    private String message;
    private String screenshotUrls;
    private String contact;
    private String pagePath;
    private String schoolSource;
    private Integer schoolId;
    private Integer status;
    private String handlingResult;
    private LocalDateTime handledAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
