package com.example.lpsx.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_activity")
public class UserActivity {
    @TableId(type = IdType.AUTO) private Long id;
    private Integer userId;
    private String schoolSource;
    private Integer schoolId;
    private String schoolName;
    private String eventType;
    private Integer durationSeconds;
    private LocalDateTime createTime;
}
