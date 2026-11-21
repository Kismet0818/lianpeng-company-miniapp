package com.example.lpsx.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class SchoolReactionVO {
    private Integer reactionType;
    private Map<String, Long> counts = new LinkedHashMap<>();
}
