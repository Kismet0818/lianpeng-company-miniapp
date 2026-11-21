package com.example.lpsx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.vo.UserFeedbackVO;

import java.util.List;
import java.util.Map;

public interface UserFeedbackService {
    UserFeedbackVO create(Integer userId, Map<String, Object> body);
    Page<UserFeedbackVO> mine(Integer userId, int page, int size);
    Page<UserFeedbackVO> adminList(int page, int size, Integer status, String category, String keyword);
    UserFeedbackVO handle(Long id, Integer status, String handlingResult);
}
