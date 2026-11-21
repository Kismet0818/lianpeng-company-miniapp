package com.example.lpsx.service;

import java.util.List;

import com.example.lpsx.vo.FavoriteVO;

/**
 * 收藏服务
 */
public interface FavoriteService {

    /**
     * 切换收藏状态（有则取消，无则收藏）
     */
    void toggle(Integer userId, String schoolSource, Integer schoolId);

    /**
     * 获取收藏列表（含用户昵称、头像、学校名称 — 三表 JOIN 一次查询）
     */
    List<FavoriteVO> list(Integer userId);
}
