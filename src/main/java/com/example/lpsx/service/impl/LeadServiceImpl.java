package com.example.lpsx.service.impl;

import org.springframework.stereotype.Service;

import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.entity.CustomerLead;
import com.example.lpsx.mapper.CustomerLeadMapper;
import com.example.lpsx.service.LeadService;

import jakarta.annotation.Resource;

/**
 * 留资服务实现 — 含 24 小时防重逻辑
 */
@Service
public class LeadServiceImpl implements LeadService {

    @Resource
    private CustomerLeadMapper customerLeadMapper;

    @Override
    public void submit(Integer userId, String parentName, String childGrade, String targetSchool,
                       String parentPhone, String concern, Integer openDayId, String reserveTime) {
        parentName = normalize(parentName, 40, "家长姓名");
        childGrade = normalize(childGrade, 30, "孩子年级");
        targetSchool = normalizeOptional(targetSchool, 100, "目标学校");
        concern = normalizeOptional(concern, 500, "咨询内容");
        reserveTime = normalizeOptional(reserveTime, 100, "预约时间");
        // 手机号 trim + 11 位格式校验（格式不合法直接拦截）
        String phone = parentPhone == null ? "" : parentPhone.trim();
        if (!phone.matches("^1\\d{10}$")) {
            throw new BusinessException("请输入正确的11位手机号");
        }

        // 防重：24 小时内同手机号是否已提交
        int count = customerLeadMapper.countByPhone24h(phone);
        if (count > 0) {
            throw new BusinessException("24小时内已提交，请勿重复提交");
        }

        CustomerLead lead = new CustomerLead();
        lead.setUserId(userId);
        lead.setParentName(parentName);
        lead.setChildGrade(childGrade);
        lead.setTargetSchool(targetSchool);
        lead.setParentPhone(phone);
        lead.setConcern(concern);
        lead.setOpenDayId(openDayId);
        lead.setReserveTime(reserveTime);
        lead.setStatus(0); // 0-待跟进

        customerLeadMapper.insert(lead);
    }

    private String normalize(String value, int maxLength, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new BusinessException(fieldName + "不能为空");
        if (normalized.length() > maxLength) throw new BusinessException(fieldName + "内容过长");
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) throw new BusinessException(fieldName + "内容过长");
        return normalized;
    }
}
