package com.example.lpsx.dto;

import lombok.Data;

@Data
public class ChannelsVideoDTO {
    private String feedId;
    private String title;
    private String cover;
    private Integer sort;
    private Integer isActive;
}
