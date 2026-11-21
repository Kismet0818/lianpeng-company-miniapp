package com.example.lpsx.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.OpenDayService;
import com.example.lpsx.vo.OpenDayVO;

import jakarta.annotation.Resource;

/**
 * 学校开放日接口 - 公开（无需 JWT）
 */
@RestController
@RequestMapping("/api/open-day")
public class OpenDayController {

    @Resource
    private OpenDayService openDayService;

    /** 开放日列表 */
    @GetMapping("/list")
    public Result<List<OpenDayVO>> list() {
        return Result.success(openDayService.list());
    }

    /** 开放日详情 */
    @GetMapping("/{id}")
    public Result<OpenDayVO> detail(@PathVariable int id) {
        return Result.success(openDayService.detail(id));
    }
}
