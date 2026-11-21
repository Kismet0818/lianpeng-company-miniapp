package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对口资料（2026年16区幼升小、小升初地段对口方案）
 */
@Data
@TableName("duikou_material")
public class DuikouMaterial {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String title;
    private String summary;
    private String content;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
