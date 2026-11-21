package com.example.lpsx.service;

import com.example.lpsx.entity.GuideArticle;
import com.example.lpsx.vo.GuideArticleDetailVO;

import java.util.List;
import java.util.Map;

public interface GuideService {

    /** 获取某分类下的二级路径列表 */
    List<String> getPaths(String category, String educationSystem);

    /** 获取某分类+路径下的文章列表 */
    List<GuideArticle> getArticles(String category, String path, String educationSystem);

    /** 文章详情 */
    GuideArticleDetailVO getArticle(int id);

    /** 获取属于指定文章的附件，并为 PDF 追加完整版领取提示页。 */
    AttachmentFile previewAttachment(int articleId, long attachmentId);

    record AttachmentFile(String fileName, String extension, String mediaType, byte[] content) {}
}
