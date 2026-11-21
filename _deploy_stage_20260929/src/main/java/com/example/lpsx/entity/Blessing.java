package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 中考祝福墙 (blessing)
 */
@Data
@TableName("blessing")
public class Blessing {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布者微信 openid */
    private String openid;

    /** 云数据库兼容字段；部分线上 blessing 表将其设为必填。 */
    @TableField("_openid")
    private String cloudOpenid;

    /** 发布者昵称 */
    private String nickname;

    /** 发布者头像 URL */
    private String avatar;

    /** 所属学校来源；旧数据为空，视为历史全局祝福。 */
    private String schoolSource;

    /** 对应来源表的学校 ID。 */
    private Integer schoolId;

    /** 发布时学校名称快照。 */
    private String schoolName;

    /** 祝福内容 */
    private String content;

    /** 状态：1-已发布 0-待审 2-隐藏 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
