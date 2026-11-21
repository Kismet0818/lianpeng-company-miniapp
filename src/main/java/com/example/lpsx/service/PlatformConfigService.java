package com.example.lpsx.service;

import java.util.Map;

/**
 * 平台配置服务
 */
public interface PlatformConfigService {

    /**
     * 获取所有配置（前端用，公开接口）
     */
    Map<String, String> getAll();

    /**
     * 批量更新配置（后台用，仅更新传入的字段）
     */
    void update(Map<String, String> configs);
}
