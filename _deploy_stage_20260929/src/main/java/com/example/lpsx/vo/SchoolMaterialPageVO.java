package com.example.lpsx.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SchoolMaterialPageVO {
    private List<SchoolMaterialListVO> records;
    private long total;
    private long page;
    private long size;
}
