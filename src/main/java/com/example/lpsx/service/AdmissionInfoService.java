package com.example.lpsx.service;

import java.util.Map;

/**
 * 招生动态服务
 */
public interface AdmissionInfoService {

    /**
     * 根据学校 ID 查询招生动态，返回结构化数据
     */
    Map<String, Object> getBySchoolId(Integer schoolId);
}
