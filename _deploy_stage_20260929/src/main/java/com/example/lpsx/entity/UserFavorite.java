package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户学校收藏表 (user_favorite)
 */
@Data
@TableName("user_favorite")
public class UserFavorite {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 用户 ID */
    private Integer userId;

    /** 用户昵称（冗余，收藏时快照） */
    private String userNickname;

    /** 学校 ID */
    private Integer schoolId;

    /** 学校数据来源，与 schoolId 共同构成学校唯一身份 */
    private String schoolSource;

    /** 学校名称（冗余，收藏时快照） */
    private String schoolName;

    /** 云数据库权限字段；使用用户 OpenID，兼容云端 NOT NULL 约束 */
    @TableField("_openid")
    private String cloudOpenid;

    /** 创建时间 */
    private LocalDateTime createTime;
}
