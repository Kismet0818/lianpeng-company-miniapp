package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.LeadService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 留资接口 �?需 JWT 鉴权
 */
@RestController
@RequestMapping("/api/lead")
public class LeadController {

    @Resource
    private LeadService leadService;

    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody Map<String, String> body,
                               @RequestAttribute("userId") Integer userId) {
        if (body == null) {
            return Result.error("提交内容不能为空");
        }
        String parentName = body.get("parent_name");
        String childGrade = body.get("child_grade");
        String targetSchool = body.get("target_school");
        String parentPhone = body.get("parent_phone");
        String concern = body.getOrDefault("concern", "");
        // 开放日预约：可选参数
        String openDayIdStr = body.get("open_day_id");
        Integer openDayId = null;
        if (openDayIdStr != null && !openDayIdStr.isBlank()) {
            try {
                openDayId = Integer.parseInt(openDayIdStr);
                if (openDayId <= 0) return Result.error("开放日编号不正确");
            } catch (NumberFormatException ignored) {
                return Result.error("开放日编号不正确");
            }
        }
        String reserveTime = body.getOrDefault("reserve_time", "");
        leadService.submit(userId, parentName, childGrade, targetSchool, parentPhone, concern, openDayId, reserveTime);
        return Result.success();
    }
}
