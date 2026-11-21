package com.example.lpsx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.common.result.Result;
import com.example.lpsx.dto.LiveSessionDTO;
import com.example.lpsx.entity.LiveSession;
import com.example.lpsx.mapper.LiveSessionMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** 直播预约场次管理；接口由管理密钥拦截器保护。 */
@RestController
@RequestMapping("/api/admin/live-sessions")
public class LiveSessionAdminController {

    @Resource
    private LiveSessionMapper liveSessionMapper;

    @GetMapping
    public Result<List<LiveSession>> list() {
        return Result.success(liveSessionMapper.selectList(new LambdaQueryWrapper<LiveSession>()
                .orderByAsc(LiveSession::getLiveTime)
                .orderByAsc(LiveSession::getSort)
                .orderByAsc(LiveSession::getId)));
    }

    @PostMapping
    public Result<LiveSession> create(@RequestBody LiveSessionDTO body) {
        LiveSession row = new LiveSession();
        apply(row, body);
        LocalDateTime now = LocalDateTime.now();
        row.setCreateTime(now);
        row.setUpdateTime(now);
        liveSessionMapper.insert(row);
        return Result.success(row);
    }

    @PutMapping("/{id}")
    public Result<LiveSession> update(@PathVariable Integer id, @RequestBody LiveSessionDTO body) {
        LiveSession row = liveSessionMapper.selectById(id);
        if (row == null) throw new BusinessException("直播场次不存在");
        apply(row, body);
        row.setUpdateTime(LocalDateTime.now());
        liveSessionMapper.updateById(row);
        return Result.success(row);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        if (liveSessionMapper.selectById(id) == null) {
            throw new BusinessException("直播场次不存在");
        }
        liveSessionMapper.deleteById(id);
        return Result.success();
    }

    private void apply(LiveSession row, LiveSessionDTO body) {
        if (body == null) throw new BusinessException("请填写直播场次信息");
        if (body.getLiveTime() == null) throw new BusinessException("请选择直播时间");

        String finderUserName = trim(body.getFinderUserName());
        String channelsName = trim(body.getChannelsName());
        if (finderUserName.isBlank()) throw new BusinessException("请填写视频号原始 ID");
        if (channelsName.isBlank()) throw new BusinessException("请填写视频号名称");
        if (finderUserName.length() > 100 || channelsName.length() > 100) {
            throw new BusinessException("视频号 ID 或名称不能超过 100 个字符");
        }

        String title = trim(body.getTitle());
        String cover = trim(body.getCover());
        String avatar = trim(body.getChannelsAvatar());
        if (title.length() > 200) throw new BusinessException("直播标题不能超过 200 个字符");
        if (cover.length() > 500 || avatar.length() > 500) {
            throw new BusinessException("图片地址不能超过 500 个字符");
        }

        row.setTitle(title.isBlank() ? channelsName + "直播" : title);
        row.setCover(cover);
        row.setLiveTime(body.getLiveTime());
        row.setFinderUserName(finderUserName);
        row.setChannelsName(channelsName);
        row.setChannelsAvatar(avatar);
        row.setSort(body.getSort() == null ? 0 : body.getSort());
        row.setIsActive(body.getIsActive() == null || body.getIsActive() != 0 ? 1 : 0);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
