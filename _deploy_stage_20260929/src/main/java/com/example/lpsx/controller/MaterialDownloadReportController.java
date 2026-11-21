package com.example.lpsx.controller;

import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.dto.MaterialSaveReportDTO;
import com.example.lpsx.service.MaterialDownloadRecordService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/material-downloads")
public class MaterialDownloadReportController {
    @Resource private MaterialDownloadRecordService service;

    @PostMapping("/report")
    public Result<Void> report(@RequestAttribute("userId") Integer userId,
                               @RequestBody MaterialSaveReportDTO body) {
        if (body == null || !"GUIDE".equalsIgnoreCase(body.getSourceType()) || body.getSourceId() == null) {
            throw new BusinessException("资料来源参数不正确");
        }
        service.recordGuideSave(userId, body.getSourceId(), body.getAttachmentId(), body.getFileUrl(), body.getFileName());
        return Result.success();
    }
}
