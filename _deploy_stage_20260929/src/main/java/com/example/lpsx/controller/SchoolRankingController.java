package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.SchoolRankingService;
import com.example.lpsx.vo.HurunRankingPageVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
public class SchoolRankingController {
    @Resource private SchoolRankingService service;

    @GetMapping("/hurun")
    public Result<HurunRankingPageVO> hurun(@RequestParam(defaultValue = "2025") int year,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "100") int size,
                                            @RequestParam(required = false) String city) {
        int safeYear = Math.min(Math.max(year, 2000), 2100);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return Result.success(service.hurun(safeYear, safePage, safeSize, city));
    }
}
