package com.example.lpsx.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 管理端新增或修改直播预约场次。 */
@Data
public class LiveSessionDTO {
    private String title;
    private String cover;
    private LocalDateTime liveTime;
    private String finderUserName;
    private String channelsName;
    private String channelsAvatar;
    private Integer sort;
    private Integer isActive;
}
