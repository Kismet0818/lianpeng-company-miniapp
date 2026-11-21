package com.example.lpsx.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.CustomerLead;
import com.example.lpsx.mapper.CustomerLeadMapper;

import jakarta.annotation.Resource;

/**
 * 咨询记录（留资）管理接口 — 需 X-Admin-Key 鉴权
 *
 * 管理端使用：
 *   GET  /api/admin/leads?status=0&keyword=张三   查询留资列表（按时间倒序）
 *   PUT  /api/admin/leads/{id}/status             更新状态 {"status": 1}
 *   DELETE /api/admin/leads/{id}                   删除单条留资（不删除用户账号）
 *
 * status: 0-待跟进, 1-已沟通, 2-已转化
 */
@RestController
@RequestMapping("/api/admin/leads")
public class LeadAdminController {

    @Resource
    private CustomerLeadMapper customerLeadMapper;

    /**
     * 查询留资列表，可按状态、关键词过滤
     *
     * @param status  状态筛选（可选）：0/1/2
     * @param keyword 关键词（可选）：匹配家长姓名 / 手机号 / 目标院校
     */
    @GetMapping
    public Result<List<CustomerLead>> list(@RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<CustomerLead> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(CustomerLead::getStatus, status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            qw.and(w -> w.like(CustomerLead::getParentName, kw)
                    .or().like(CustomerLead::getParentPhone, kw)
                    .or().like(CustomerLead::getTargetSchool, kw));
        }
        // 最新提交的排前面
        qw.orderByDesc(CustomerLead::getCreateTime);
        return Result.success(customerLeadMapper.selectList(qw));
    }

    /**
     * 更新留资状态
     *
     * @param id   记录 ID
     * @param body {"status": 1}
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Integer id,
                                     @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        // 仅允许 0/1/2 三种合法状态
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            return Result.error("状态不合法，仅支持 0-待跟进 / 1-已沟通 / 2-已转化");
        }
        CustomerLead lead = customerLeadMapper.selectById(id);
        if (lead == null) {
            return Result.error("记录不存在");
        }
        lead.setStatus(status);
        customerLeadMapper.updateById(lead);
        return Result.success();
    }

    /** 删除单条留资记录，不影响用户账号、浏览、收藏或评价数据。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        CustomerLead lead = customerLeadMapper.selectById(id);
        if (lead == null) {
            return Result.error("记录不存在或已删除");
        }
        customerLeadMapper.deleteById(id);
        return Result.success();
    }
}
