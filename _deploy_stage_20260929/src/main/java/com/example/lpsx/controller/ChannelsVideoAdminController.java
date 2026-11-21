package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.dto.ChannelsVideoDTO;
import com.example.lpsx.entity.ChannelsVideo;
import com.example.lpsx.mapper.ChannelsVideoMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/channels-videos")
public class ChannelsVideoAdminController {
    @Resource private ChannelsVideoMapper mapper;
    @GetMapping public Result<List<ChannelsVideo>> list() { return Result.success(mapper.selectList(new LambdaQueryWrapper<ChannelsVideo>().orderByAsc(ChannelsVideo::getSort).orderByAsc(ChannelsVideo::getId))); }
    @PostMapping public Result<ChannelsVideo> create(@RequestBody ChannelsVideoDTO body) { ChannelsVideo row = new ChannelsVideo(); apply(row, body); mapper.insert(row); return Result.success(row); }
    @PutMapping("/{id}") public Result<ChannelsVideo> update(@PathVariable Integer id, @RequestBody ChannelsVideoDTO body) { ChannelsVideo row = mapper.selectById(id); if (row == null) throw new BusinessException("视频号短视频不存在"); apply(row, body); mapper.updateById(row); return Result.success(row); }
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Integer id) { if (mapper.selectById(id) == null) throw new BusinessException("视频号短视频不存在"); mapper.deleteById(id); return Result.success(); }
    private void apply(ChannelsVideo row, ChannelsVideoDTO body) { if (body == null || blank(body.getFeedId())) throw new BusinessException("请填写视频 feed_id"); if (blank(body.getTitle())) throw new BusinessException("请填写视频标题"); if (body.getFeedId().trim().length() > 64 || body.getTitle().trim().length() > 128) throw new BusinessException("feed_id 或标题长度超限"); row.setFeedId(body.getFeedId().trim()); row.setTitle(body.getTitle().trim()); row.setCover(body.getCover() == null ? "" : body.getCover().trim()); row.setSort(body.getSort() == null ? 0 : body.getSort()); row.setIsActive(body.getIsActive() == null || body.getIsActive() != 0 ? 1 : 0); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
