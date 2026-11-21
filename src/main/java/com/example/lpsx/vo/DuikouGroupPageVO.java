package com.example.lpsx.vo;

import lombok.Data;

import java.util.List;

@Data
public class DuikouGroupPageVO {
    private long current;
    private long size;
    private long total;
    private long pages;
    private List<DuikouSchoolGroupVO> records;
}
