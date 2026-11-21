package com.example.lpsx.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageResultVO<T> {
    private List<T> records;
    private long total;
    private long current;
    private long size;
    private long pages;
}
