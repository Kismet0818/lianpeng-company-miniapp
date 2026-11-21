package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 视频号视频列表 (channels_video)
 */
@Data
@TableName("channels_video")
public class ChannelsVideo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 视频 feedId */
    private String feedId;

    /** 视频标题 */
    private String title;

    /** 封面图 OSS 地址 */
    private String cover;

    /** 排序 */
    private Integer sort;

    /** 是否显示 */
    private Integer isActive;
}
