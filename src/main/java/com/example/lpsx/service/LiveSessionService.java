package com.example.lpsx.service;

import com.example.lpsx.entity.LiveSession;

import java.util.List;

/**
 * 视频号直播服务
 */
public interface LiveSessionService {

    /**
     * 直播列表（is_active=1，按 sort 升序）
     */
    List<LiveSession> list();
}
