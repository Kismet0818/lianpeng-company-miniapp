package com.example.lpsx.service;

import com.example.lpsx.vo.OpenDayVO;

import java.util.List;

/**
 * 学校开放日服务
 */
public interface OpenDayService {

    /**
     * 开放日列表（含学校 Logo）
     */
    List<OpenDayVO> list();

    /**
     * 开放日详情（含学校 Logo）
     */
    OpenDayVO detail(int id);
}
