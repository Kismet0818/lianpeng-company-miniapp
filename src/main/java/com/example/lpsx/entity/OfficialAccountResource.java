package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("official_account_resource")
public class OfficialAccountResource {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private String url;
    private Integer sortOrder;
    private Integer isEnabled;
    private String coverUrl;
}
