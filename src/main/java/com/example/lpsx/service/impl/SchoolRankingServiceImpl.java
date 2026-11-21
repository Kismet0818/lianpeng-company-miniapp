package com.example.lpsx.service.impl;

import com.example.lpsx.mapper.SchoolRankingMapper;
import com.example.lpsx.service.SchoolRankingService;
import com.example.lpsx.vo.HurunRankingPageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SchoolRankingServiceImpl implements SchoolRankingService {
    private static final String SUBTITLE = "Hurun Education Top International Schools in China 2025";
    @Resource private SchoolRankingMapper mapper;

    @Override
    public HurunRankingPageVO hurun(int year, int page, int size, String city) {
        String normalizedCity = normalizeCity(city);
        long total = mapper.countHurun(year, normalizedCity);
        HurunRankingPageVO result = new HurunRankingPageVO();
        result.setRanking("hurun"); result.setYear(String.valueOf(year));
        result.setTitle(mapper.selectTitle(year));
        result.setSubtitle(year == 2025 ? SUBTITLE : "Hurun Education Top International Schools in China " + year);
        result.setRecords(mapper.selectHurun(year, normalizedCity, (page - 1) * size, size));
        result.setTotal(total); result.setPage(page); result.setSize(size);
        return result;
    }

    private String normalizeCity(String city) {
        if (city == null || city.isBlank()) return null;
        String value = city.trim();
        return value.endsWith("市") ? value.substring(0, value.length() - 1) : value;
    }
}
