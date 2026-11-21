package com.example.lpsx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.entity.User;
import com.example.lpsx.entity.UserFeedback;
import com.example.lpsx.mapper.UserFeedbackMapper;
import com.example.lpsx.mapper.UserMapper;
import com.example.lpsx.service.UserFeedbackService;
import com.example.lpsx.vo.UserFeedbackVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserFeedbackServiceImpl implements UserFeedbackService {
    private static final Set<String> CATEGORIES = Set.of("correction", "content", "product");
    private static final Set<String> SCHOOL_SOURCES = Set.of("international", "score", "junior", "general", "primary", "kindergarten");
    private static final Set<Integer> STATUSES = Set.of(0, 1, 2, 3);
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_CONTACT_LENGTH = 100;
    private static final int MAX_PAGE_PATH_LENGTH = 500;
    private static final int MAX_SCREENSHOTS = 4;

    @Resource private UserFeedbackMapper feedbackMapper;
    @Resource private UserMapper userMapper;
    @Resource private ObjectMapper objectMapper;

    @Override
    public UserFeedbackVO create(Integer userId, Map<String, Object> body) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getOpenid())) {
            throw new BusinessException("登录信息已失效，请重新登录");
        }
        String category = text(body, "category");
        if (category != null) category = category.toLowerCase();
        if (!CATEGORIES.contains(category)) throw new BusinessException("反馈类型不正确");
        String message = text(body, "message");
        if (message == null || message.length() < 5) throw new BusinessException("留言至少填写 5 个字");
        if (message.length() > MAX_MESSAGE_LENGTH) throw new BusinessException("留言最多 2000 个字");
        String contact = optionalText(body, "contact", MAX_CONTACT_LENGTH, "联系方式最多 100 个字");
        String pagePath = optionalText(body, "pagePath", MAX_PAGE_PATH_LENGTH, "页面路径过长");
        String schoolSource = optionalText(body, "schoolSource", 20, "学校来源过长");
        Integer schoolId = positiveInt(body == null ? null : body.get("schoolId"));
        if (schoolSource != null) schoolSource = schoolSource.toLowerCase();
        if ((schoolSource == null) != (schoolId == null) ||
                (schoolSource != null && !SCHOOL_SOURCES.contains(schoolSource))) {
            throw new BusinessException("关联学校参数不正确");
        }
        List<String> screenshots = screenshots(body == null ? null : body.get("screenshotUrls"));

        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setCloudOpenid(user.getOpenid());
        feedback.setCategory(category);
        feedback.setMessage(message);
        feedback.setScreenshotUrls(writeScreenshots(screenshots));
        feedback.setContact(contact);
        feedback.setPagePath(pagePath);
        feedback.setSchoolSource(schoolSource);
        feedback.setSchoolId(schoolId);
        feedback.setStatus(0);
        LocalDateTime now = LocalDateTime.now();
        feedback.setCreateTime(now);
        feedback.setUpdateTime(now);
        feedbackMapper.insert(feedback);
        return toVO(feedback);
    }

    @Override
    public Page<UserFeedbackVO> mine(Integer userId, int page, int size) {
        LambdaQueryWrapper<UserFeedback> query = new LambdaQueryWrapper<UserFeedback>()
                .eq(UserFeedback::getUserId, userId)
                .orderByDesc(UserFeedback::getCreateTime).orderByDesc(UserFeedback::getId);
        return convert(feedbackMapper.selectPage(new Page<>(safePage(page), safeSize(size)), query));
    }

    @Override
    public Page<UserFeedbackVO> adminList(int page, int size, Integer status, String category, String keyword) {
        LambdaQueryWrapper<UserFeedback> query = new LambdaQueryWrapper<>();
        if (status != null) {
            if (!STATUSES.contains(status)) throw new BusinessException("处理状态不正确");
            query.eq(UserFeedback::getStatus, status);
        }
        if (StringUtils.hasText(category)) {
            String value = category.trim().toLowerCase();
            if (!CATEGORIES.contains(value)) throw new BusinessException("反馈类型不正确");
            query.eq(UserFeedback::getCategory, value);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(q -> q.like(UserFeedback::getMessage, value).or().like(UserFeedback::getContact, value));
        }
        query.orderByAsc(UserFeedback::getStatus).orderByDesc(UserFeedback::getCreateTime);
        return convert(feedbackMapper.selectPage(new Page<>(safePage(page), safeSize(size)), query));
    }

    @Override
    public UserFeedbackVO handle(Long id, Integer status, String handlingResult) {
        UserFeedback feedback = id == null ? null : feedbackMapper.selectById(id);
        if (feedback == null) throw new BusinessException("反馈记录不存在");
        if (status == null || !STATUSES.contains(status)) throw new BusinessException("处理状态不正确");
        String result = handlingResult == null ? null : handlingResult.trim();
        if (result != null && result.length() > 2000) throw new BusinessException("处理结果最多 2000 个字");
        if ((status == 2 || status == 3) && !StringUtils.hasText(result)) {
            throw new BusinessException("办结或关闭时请填写处理结果");
        }
        feedback.setStatus(status);
        feedback.setHandlingResult(result);
        feedback.setHandledAt(status == 2 || status == 3 ? LocalDateTime.now() : null);
        feedback.setUpdateTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
        return toVO(feedback);
    }

    private Page<UserFeedbackVO> convert(Page<UserFeedback> source) {
        Page<UserFeedbackVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    private UserFeedbackVO toVO(UserFeedback row) {
        return new UserFeedbackVO(row.getId(), row.getCategory(), row.getMessage(), readScreenshots(row.getScreenshotUrls()),
                row.getContact(), row.getPagePath(), row.getSchoolSource(), row.getSchoolId(), row.getStatus(),
                row.getHandlingResult(), row.getHandledAt(), row.getCreateTime(), row.getUpdateTime());
    }

    private List<String> screenshots(Object value) {
        if (value == null) return List.of();
        if (!(value instanceof List<?> list)) throw new BusinessException("截图参数格式不正确");
        if (list.size() > MAX_SCREENSHOTS) throw new BusinessException("最多上传 4 张截图");
        List<String> urls = new ArrayList<>();
        for (Object item : list) {
            String url = item == null ? null : String.valueOf(item).trim();
            if (!StringUtils.hasText(url) || url.length() > 1000 || !url.startsWith("https://")) {
                throw new BusinessException("截图地址不正确");
            }
            urls.add(url);
        }
        return urls;
    }

    private String writeScreenshots(List<String> urls) {
        try { return objectMapper.writeValueAsString(urls); }
        catch (Exception e) { throw new BusinessException("截图信息保存失败"); }
    }

    private List<String> readScreenshots(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of(); }
    }

    private String text(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) return null;
        String value = String.valueOf(body.get(key)).trim();
        return value.isEmpty() ? null : value;
    }

    private String optionalText(Map<String, Object> body, String key, int max, String error) {
        if (body == null || body.get(key) == null) return null;
        String value = String.valueOf(body.get(key)).trim();
        if (value.isEmpty()) return null;
        if (value.length() > max) throw new BusinessException(error);
        return value;
    }

    private Integer positiveInt(Object value) {
        if (value == null) return null;
        try { int number = Integer.parseInt(String.valueOf(value)); return number > 0 ? number : null; }
        catch (Exception e) { throw new BusinessException("关联学校编号不正确"); }
    }

    private int safePage(int page) { return Math.max(1, page); }
    private int safeSize(int size) { return size < 1 || size > 50 ? 20 : size; }
}
