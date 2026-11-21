package com.example.lpsx.service;

/**
 * 留资服务
 */
public interface LeadService {

    /**
     * 提交客户留资信息
     *
     * @param userId           用户 ID
     * @param parentName       家长姓名
     * @param childGrade       当前年级
     * @param targetSchool     目标院校
     * @param parentPhone      联系电话
     * @param concern          用户疑问
     * @param openDayId        关联开放日 ID (可空)
     * @param reserveTime      预约日期 (可空)
     */
    void submit(Integer userId, String parentName, String childGrade, String targetSchool,
                String parentPhone, String concern, Integer openDayId, String reserveTime);
}
