package com.example.lpsx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.entity.AdmissionContentArticle;
import com.example.lpsx.mapper.AdmissionContentArticleMapper;
import com.example.lpsx.service.AdmissionContentService;
import com.example.lpsx.vo.AdmissionContentArticleDetailVO;
import com.example.lpsx.vo.AdmissionContentArticleListVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdmissionContentServiceImpl implements AdmissionContentService {
    private static final String COMPLETE = "text_complete";
    @Resource private AdmissionContentArticleMapper mapper;
    @Resource private ObjectMapper objectMapper;

    @Override
    public Page<AdmissionContentArticleListVO> list(String category, Integer year, int page, int size) {
        LambdaQueryWrapper<AdmissionContentArticle> query = new LambdaQueryWrapper<>();
        query.select(AdmissionContentArticle::getId, AdmissionContentArticle::getSourceCategory,
                        AdmissionContentArticle::getTitle, AdmissionContentArticle::getArticleYear,
                        AdmissionContentArticle::getContentFormat, AdmissionContentArticle::getOssImageUrls,
                        AdmissionContentArticle::getImageCount, AdmissionContentArticle::getSortOrder)
                .eq(AdmissionContentArticle::getSourceCategory, category)
                .eq(AdmissionContentArticle::getStatus, 1)
                .eq(AdmissionContentArticle::getCaptureStatus, COMPLETE)
                .eq(year != null && "详细文章".equals(category), AdmissionContentArticle::getArticleYear, year)
                .orderByAsc(AdmissionContentArticle::getSortOrder).orderByAsc(AdmissionContentArticle::getId);
        Page<AdmissionContentArticle> rows = mapper.selectPage(new Page<>(page, size), query);
        Page<AdmissionContentArticleListVO> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(rows.getRecords().stream().map(this::toListVO).toList());
        return result;
    }

    @Override
    public AdmissionContentArticleDetailVO detail(long id) {
        AdmissionContentArticle article = mapper.selectOne(new LambdaQueryWrapper<AdmissionContentArticle>()
                .eq(AdmissionContentArticle::getId, id).eq(AdmissionContentArticle::getStatus, 1)
                .eq(AdmissionContentArticle::getCaptureStatus, COMPLETE));
        if (article == null) return null;
        AdmissionContentArticleDetailVO result = new AdmissionContentArticleDetailVO();
        result.setId(article.getId()); result.setCategory(article.getSourceCategory());
        result.setTitle(article.getTitle()); result.setYear(article.getArticleYear());
        result.setContentFormat(article.getContentFormat()); result.setContentText(article.getContentText());
        result.setImageUrls(parseImageUrls(article.getOssImageUrls()));
        result.setImageCount(result.getImageUrls().size());
        return result;
    }

    private AdmissionContentArticleListVO toListVO(AdmissionContentArticle article) {
        List<String> images = parseImageUrls(article.getOssImageUrls());
        AdmissionContentArticleListVO result = new AdmissionContentArticleListVO();
        result.setId(article.getId()); result.setCategory(article.getSourceCategory());
        result.setTitle(article.getTitle()); result.setYear(article.getArticleYear());
        result.setContentFormat(article.getContentFormat());
        result.setCoverImageUrl(images.isEmpty() ? null : images.get(0));
        result.setImageCount(images.size()); result.setSortOrder(article.getSortOrder());
        return result;
    }

    private List<String> parseImageUrls(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<String> urls = objectMapper.readValue(json, new TypeReference<>() {});
            return urls == null ? new ArrayList<>() : urls;
        } catch (Exception exception) {
            throw new IllegalStateException("文章图片数据格式错误", exception);
        }
    }
}
