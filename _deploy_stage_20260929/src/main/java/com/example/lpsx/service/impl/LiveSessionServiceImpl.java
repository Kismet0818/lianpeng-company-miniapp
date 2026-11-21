package com.example.lpsx.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.LiveSession;
import com.example.lpsx.mapper.LiveSessionMapper;
import com.example.lpsx.service.LiveSessionService;

import jakarta.annotation.Resource;

/**
 * 视频号直播服务实现
 */
@Service
public class LiveSessionServiceImpl implements LiveSessionService {

    @Resource
    private LiveSessionMapper liveSessionMapper;

    @Override
    public List<LiveSession> list() {
        return liveSessionMapper.selectList(
                new LambdaQueryWrapper<LiveSession>()
                        .eq(LiveSession::getIsActive, 1)
                        .orderByAsc(LiveSession::getSort)
                        .orderByAsc(LiveSession::getId));
    }
}
