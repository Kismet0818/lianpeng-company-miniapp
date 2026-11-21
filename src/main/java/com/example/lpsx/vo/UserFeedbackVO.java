package com.example.lpsx.vo;

import java.time.LocalDateTime;
import java.util.List;

public record UserFeedbackVO(
        Long id,
        String category,
        String message,
        List<String> screenshotUrls,
        String contact,
        String pagePath,
        String schoolSource,
        Integer schoolId,
        Integer status,
        String handlingResult,
        LocalDateTime handledAt,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
