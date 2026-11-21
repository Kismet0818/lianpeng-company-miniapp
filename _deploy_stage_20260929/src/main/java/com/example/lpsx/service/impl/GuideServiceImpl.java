package com.example.lpsx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.GuideArticle;
import com.example.lpsx.mapper.GuideAttachmentMapper;
import com.example.lpsx.mapper.GuideArticleMapper;
import com.example.lpsx.service.GuideService;
import com.example.lpsx.service.PdfPreviewPageService;
import com.example.lpsx.vo.GuideArticleDetailVO;
import com.example.lpsx.vo.GuideAttachmentVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GuideServiceImpl implements GuideService {

    private static final Set<String> EDUCATION_SYSTEMS = Set.of("internal", "external");
    private static final Map<String, Integer> PLANNING_PATH_ORDER = Map.of(
            "升学路径", 1,
            "规划须知/志愿填报小知识", 2,
            "学校介绍", 3,
            "课程体系", 4
    );

    private static final Pattern LEGACY_ATTACHMENT_HTML = Pattern.compile(
            "(?is)<p><strong>政策附件：</strong>\\s*<a\\s+href=\"[^\"]*\">.*?</a></p>\\s*"
                    + "<p>https?://[^<]+</p>");

    @Resource
    private GuideArticleMapper guideArticleMapper;

    @Resource
    private GuideAttachmentMapper guideAttachmentMapper;

    @Resource
    private PdfPreviewPageService pdfPreviewPageService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public List<String> getPaths(String category, String educationSystem) {
        String system = normalizeEducationSystem(educationSystem);
        LambdaQueryWrapper<GuideArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GuideArticle::getCategory, category)
               .eq(GuideArticle::getStatus, 1)
               .eq(system != null, GuideArticle::getEducationSystem, system)
               .orderByAsc(GuideArticle::getSortOrder);
        List<String> paths = guideArticleMapper.selectList(wrapper).stream()
                .map(GuideArticle::getPathName)
                .distinct()
                .collect(Collectors.toList());
        if ("升学规划".equals(category)) {
            paths.sort(java.util.Comparator
                    .comparingInt((String path) -> PLANNING_PATH_ORDER.getOrDefault(path, Integer.MAX_VALUE))
                    .thenComparing(path -> path));
        }
        return paths;
    }

    @Override
    public List<GuideArticle> getArticles(String category, String path, String educationSystem) {
        String system = normalizeEducationSystem(educationSystem);
        LambdaQueryWrapper<GuideArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(category), GuideArticle::getCategory, category)
               .eq(GuideArticle::getStatus, 1)
               .eq(StringUtils.hasText(path), GuideArticle::getPathName, path)
               .eq(system != null, GuideArticle::getEducationSystem, system)
               // 体制内文章优先；同体系内沿用业务排序，最后用 ID 保证顺序稳定。
               .last("ORDER BY CASE WHEN education_system = 'internal' THEN 0 ELSE 1 END, sort_order ASC, id ASC");
        return guideArticleMapper.selectList(wrapper);
    }

    @Override
    public GuideArticleDetailVO getArticle(int id) {
        GuideArticle article = guideArticleMapper.selectOne(new LambdaQueryWrapper<GuideArticle>()
                .eq(GuideArticle::getId, id).eq(GuideArticle::getStatus, 1));
        if (article == null) return null;

        GuideArticleDetailVO detail = new GuideArticleDetailVO();
        BeanUtils.copyProperties(article, detail);

        List<GuideAttachmentVO> attachments = guideAttachmentMapper.selectByGuideArticleId(id);
        detail.setAttachments(attachments == null ? new ArrayList<>() : attachments);

        // 导入政策时曾把附件链接和裸 URL 拼入正文。新接口已结构化返回附件，
        // 详情正文移除这段历史 HTML，避免小程序继续显示一长串网址。
        if (!detail.getAttachments().isEmpty() && StringUtils.hasText(detail.getContent())) {
            detail.setContent(LEGACY_ATTACHMENT_HTML.matcher(detail.getContent()).replaceAll(""));
        }
        return detail;
    }

    @Override
    public AttachmentFile previewAttachment(int articleId, long attachmentId) {
        GuideArticle article = guideArticleMapper.selectById(articleId);
        if (article == null) return null;
        GuideAttachmentVO attachment = guideAttachmentMapper.selectOwnedAttachment(articleId, attachmentId);
        if (attachment == null || !StringUtils.hasText(attachment.getFileUrl())) return null;
        URI uri;
        try {
            uri = URI.create(attachment.getFileUrl().trim());
        } catch (IllegalArgumentException error) {
            return null;
        }
        // 附件地址来自数据库，但仍限制为 HTTPS，避免该公开接口成为任意地址代理。
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(uri.getHost())
                || !uri.getHost().toLowerCase().endsWith(".aliyuncs.com")) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length == 0 || response.body().length > 30 * 1024 * 1024) {
                return null;
            }
            byte[] content = response.body();
            String extension = attachment.getFileExtension();
            String mediaType = attachment.getMediaType();
            boolean isPdf = "pdf".equalsIgnoreCase(extension) || "application/pdf".equalsIgnoreCase(mediaType);
            boolean showFullContent = "升学规划".equals(article.getCategory());
            if (isPdf && !showFullContent) {
                content = pdfPreviewPageService.firstPageWithClaimPage(content);
                mediaType = "application/pdf";
                extension = "pdf";
            } else if (isPdf) {
                // “择校指南 → 升学规划”展示 OSS 中的完整 PDF，不截页、不追加领取提示页。
                mediaType = "application/pdf";
                extension = "pdf";
            }
            return new AttachmentFile(attachment.getFileName(), extension, mediaType, content);
        } catch (Exception error) {
            return null;
        }
    }

    private String normalizeEducationSystem(String value) {
        if (!StringUtils.hasText(value)) return null;
        String system = value.trim().toLowerCase();
        if (!EDUCATION_SYSTEMS.contains(system)) {
            throw new com.example.lpsx.common.exception.BusinessException("升学体系参数不正确");
        }
        return system;
    }
}
