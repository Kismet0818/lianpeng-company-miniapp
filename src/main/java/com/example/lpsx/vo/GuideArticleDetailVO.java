package com.example.lpsx.vo;

import java.util.ArrayList;
import java.util.List;

import com.example.lpsx.entity.GuideArticle;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文章详情；在原有字段基础上增加结构化附件，兼容现有前端。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GuideArticleDetailVO extends GuideArticle {
    private List<GuideAttachmentVO> attachments = new ArrayList<>();
}
