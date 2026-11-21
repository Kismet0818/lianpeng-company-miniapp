package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.AdmissionContentArticle;
import com.example.lpsx.entity.DuikouMaterial;
import com.example.lpsx.entity.LotteryGuide;
import com.example.lpsx.entity.ParentQa;
import com.example.lpsx.mapper.AdmissionContentArticleMapper;
import com.example.lpsx.mapper.DuikouMaterialMapper;
import com.example.lpsx.mapper.LotteryGuideMapper;
import com.example.lpsx.mapper.ParentQaMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 首页四类资料的启用/停用管理：status=1 启用，status=0 停用。 */
@RestController
@RequestMapping("/api/admin/home-content")
public class ContentStatusAdminController {
    @Resource private DuikouMaterialMapper duikouMapper;
    @Resource private AdmissionContentArticleMapper policyMapper;
    @Resource private ParentQaMapper qaMapper;
    @Resource private LotteryGuideMapper lotteryMapper;

    @PutMapping("/{type}/{id}/status")
    public Result<Void> updateStatus(@PathVariable String type, @PathVariable long id,
                                     @RequestBody Map<String, Integer> body) {
        Integer status = body == null ? null : body.get("status");
        if (status == null || (status != 0 && status != 1)) return Result.error("status 只支持 0 或 1");
        int updated;
        switch (type) {
            case "duikou" -> { DuikouMaterial x = duikouMapper.selectById(id); if (x == null) return Result.error(404, "资料不存在"); x.setStatus(status); updated = duikouMapper.updateById(x); }
            case "policy" -> { AdmissionContentArticle x = policyMapper.selectById(id); if (x == null) return Result.error(404, "政策不存在"); x.setStatus(status); updated = policyMapper.updateById(x); }
            case "parent-qa" -> { ParentQa x = qaMapper.selectById(id); if (x == null) return Result.error(404, "问答不存在"); x.setStatus(status); updated = qaMapper.updateById(x); }
            case "lottery-guide" -> { LotteryGuide x = lotteryMapper.selectById(id); if (x == null) return Result.error(404, "指南不存在"); x.setStatus(status); updated = lotteryMapper.updateById(x); }
            default -> { return Result.error("type 不正确"); }
        }
        return updated > 0 ? Result.success() : Result.error("状态更新失败");
    }
}
