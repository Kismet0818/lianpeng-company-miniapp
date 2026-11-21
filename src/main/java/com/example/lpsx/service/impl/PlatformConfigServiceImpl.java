package com.example.lpsx.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.PlatformConfig;
import com.example.lpsx.mapper.PlatformConfigMapper;
import com.example.lpsx.service.PlatformConfigService;

import jakarta.annotation.Resource;

/**
 * 平台配置服务实现
 */
@Service
public class PlatformConfigServiceImpl implements PlatformConfigService {

    @Resource
    private PlatformConfigMapper configMapper;

    @Override
    public Map<String, String> getAll() {
        List<PlatformConfig> list = configMapper.selectList(null);
        Map<String, String> result = new LinkedHashMap<>();
        for (PlatformConfig c : list) {
            result.put(c.getConfigKey(), c.getConfigValue());
        }
        return result;
    }

    @Override
    public void update(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            LambdaQueryWrapper<PlatformConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PlatformConfig::getConfigKey, entry.getKey());
            PlatformConfig exist = configMapper.selectOne(wrapper);
            if (exist != null) {
                exist.setConfigValue(entry.getValue());
                configMapper.updateById(exist);
            }
        }
    }
}
