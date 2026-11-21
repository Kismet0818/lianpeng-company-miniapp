package com.example.lpsx.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.lpsx.entity.GuideArticle;
import com.example.lpsx.mapper.GuideArticleMapper;
import com.example.lpsx.mapper.GuideAttachmentMapper;
import com.example.lpsx.vo.GuideAttachmentVO;

@ExtendWith(MockitoExtension.class)
class GuideServiceImplTest {

    @Mock private GuideArticleMapper guideArticleMapper;
    @Mock private GuideAttachmentMapper guideAttachmentMapper;
    @InjectMocks private GuideServiceImpl guideService;

    @Test
    void returnsStructuredAttachmentsAndRemovesLegacyUrlBlock() {
        GuideArticle article = new GuideArticle();
        article.setId(71);
        article.setTitle("测试政策");
        article.setContent("<p>正文</p><p><strong>政策附件：</strong><a href=\"https://oss/a.pdf\">附件.pdf</a></p><p>https://oss/a.pdf</p>");

        GuideAttachmentVO attachment = new GuideAttachmentVO();
        attachment.setId(1L);
        attachment.setFileName("附件.pdf");
        attachment.setFileUrl("https://oss/a.pdf");
        attachment.setFileExtension("pdf");

        when(guideArticleMapper.selectById(71)).thenReturn(article);
        when(guideAttachmentMapper.selectByGuideArticleId(71)).thenReturn(List.of(attachment));

        var detail = guideService.getArticle(71);

        assertEquals("测试政策", detail.getTitle());
        assertEquals("<p>正文</p>", detail.getContent());
        assertEquals(1, detail.getAttachments().size());
        assertEquals("附件.pdf", detail.getAttachments().get(0).getFileName());
    }

    @Test
    void keepsOrdinaryArticleContentAndReturnsEmptyAttachmentList() {
        GuideArticle article = new GuideArticle();
        article.setId(1);
        article.setContent("<p>普通文章</p>");

        when(guideArticleMapper.selectById(1)).thenReturn(article);
        when(guideAttachmentMapper.selectByGuideArticleId(1)).thenReturn(List.of());

        var detail = guideService.getArticle(1);

        assertEquals("<p>普通文章</p>", detail.getContent());
        assertFalse(detail.getAttachments().iterator().hasNext());
    }

    @Test
    void returnsNullWhenArticleDoesNotExist() {
        when(guideArticleMapper.selectById(999)).thenReturn(null);
        assertNull(guideService.getArticle(999));
    }
}
