package com.example.lpsx.vo;

/**
 * 查分工具学校列表项。
 */
public record ScoreQueryToolSchoolListVO(
        Integer id,
        String name,
        String shortName,
        String logo,
        String district,
        String tier) {
}