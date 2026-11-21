package com.example.lpsx.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** 公开祝福墙列表项，不暴露 openid 和内部审核状态。 */
public record BlessingListVO(
        Long id,
        String nickname,
        String avatar,
        String schoolSource,
        Integer schoolId,
        String schoolName,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createTime) {
}
