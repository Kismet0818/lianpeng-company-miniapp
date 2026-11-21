package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频号直播表 (live_session)
 */
@Data
@TableName("live_session")
public class LiveSession {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 直播标题（如：雅思课程教学） */
    private String title;

    /** 封面图 OSS 地址 */
    private String cover;

    /** 直播时间 */
    private LocalDateTime liveTime;

    /** 视频号 ID（预约直播用） */
    private String finderUserName;

    /** 视频号名称 */
    private String channelsName;

    /** 视频号头像 OSS 地址 */
    private String channelsAvatar;

    /** 排序（越小越前） */
    private Integer sort;

    /** 是否显示: 1-显示, 0-隐藏 */
    private Integer isActive;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
