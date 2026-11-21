package com.example.lpsx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.vo.AdmissionContentArticleDetailVO;
import com.example.lpsx.vo.AdmissionContentArticleListVO;

public interface AdmissionContentService {
    Page<AdmissionContentArticleListVO> list(String category, Integer year, int page, int size);
    AdmissionContentArticleDetailVO detail(long id);
}
