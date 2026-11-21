package com.example.lpsx.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class SchoolCompareRef {
    @JsonAlias("source")
    private String schoolSource;
    private Integer id;
}
