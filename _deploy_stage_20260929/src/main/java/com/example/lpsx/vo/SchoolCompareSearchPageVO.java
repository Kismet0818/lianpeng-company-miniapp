package com.example.lpsx.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchoolCompareSearchPageVO {
    private List<SchoolCompareVO> records;
    private int page;
    private int size;
    private long total;
    private boolean hasMore;
}
