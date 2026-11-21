package com.example.lpsx.vo;

import lombok.Data;

import java.util.List;

@Data
public class HurunRankingPageVO {
    private String ranking;
    private String year;
    private String title;
    private String subtitle;
    private List<HurunRankingRecordVO> records;
    private long total;
    private int page;
    private int size;
}
