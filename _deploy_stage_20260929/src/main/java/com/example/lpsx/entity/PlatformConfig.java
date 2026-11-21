package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台配置表 (platform_config)
 */
@Data
@TableName("platform_config")
public class PlatformConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
