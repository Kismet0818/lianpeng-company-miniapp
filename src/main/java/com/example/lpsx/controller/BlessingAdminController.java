package com.example.lpsx.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.Blessing;
import com.example.lpsx.mapper.BlessingMapper;

import jakarta.annotation.Resource;

/**
 * 祝福墙管理接口 — 需 X-Admin-Key 鉴权（AdminKeyInterceptor 保护 /api/admin/**）
 *
 *   GET     /api/admin/blessing          全部祝福（含隐藏/待审，按时间倒序）
 *   DELETE  /api/admin/blessing/{id}     删除违规祝福
 */
@RestController
@RequestMapping("/api/admin/blessing")
public class BlessingAdminController {

    @Resource
    private BlessingMapper blessingMapper;

    @GetMapping
    public Result<List<Blessing>> list() {
        LambdaQueryWrapper<Blessing> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(Blessing::getCreateTime);
        return Result.success(blessingMapper.selectList(qw));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (blessingMapper.selectById(id) == null) {
            return Result.error("记录不存在");
        }
        blessingMapper.deleteById(id);
        return Result.success();
    }
}
