package com.example.lpsx.service;

import com.example.lpsx.vo.HurunRankingPageVO;

public interface SchoolRankingService {
    HurunRankingPageVO hurun(int year, int page, int size, String city);
}
