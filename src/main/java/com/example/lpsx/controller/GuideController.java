package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.GuideArticle;
import com.example.lpsx.service.GuideService;
import com.example.lpsx.vo.GuideArticleDetailVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/guide")
public class GuideController {

    @Resource
    private GuideService guideService;

    @Value("${app.material-public-base-url:}")
    private String configuredPublicBaseUrl;

    @GetMapping("/paths")
    public Result<List<String>> paths(@RequestParam String category,
                                      @RequestParam(required = false) String educationSystem) {
        return Result.success(guideService.getPaths(category, educationSystem));
    }

    @GetMapping("/articles")
    public Result<List<GuideArticle>> articles(@RequestParam String category,
                                                @RequestParam(required = false) String path,
                                                @RequestParam(required = false) String educationSystem) {
        return Result.success(guideService.getArticles(category, path, educationSystem));
    }

    @GetMapping("/article/{id}")
    public Result<GuideArticleDetailVO> article(@PathVariable int id, HttpServletRequest request) {
        GuideArticleDetailVO detail = guideService.getArticle(id);
        if (detail != null && detail.getAttachments() != null) {
            String baseUrl = publicBaseUrl(request);
            detail.getAttachments().stream()
                    .filter(item -> "pdf".equalsIgnoreCase(item.getFileExtension()))
                    .forEach(item -> item.setFileUrl(baseUrl + "/api/guide/article/" + id
                            + "/attachments/" + item.getId() + "/preview"));
        }
        return Result.success(detail);
    }

    @GetMapping("/article/{articleId}/attachments/{attachmentId}/preview")
    public ResponseEntity<byte[]> previewAttachment(@PathVariable int articleId,
                                                     @PathVariable long attachmentId) {
        GuideService.AttachmentFile file = guideService.previewAttachment(articleId, attachmentId);
        if (file == null) return ResponseEntity.notFound().build();
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mediaType()); }
        catch (Exception ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName() == null || file.fileName().isBlank()
                        ? "guide-preview." + (file.extension() == null ? "pdf" : file.extension())
                        : file.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType).contentLength(file.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.content());
    }

    private String publicBaseUrl(HttpServletRequest request) {
        if (configuredPublicBaseUrl != null && !configuredPublicBaseUrl.isBlank()) {
            return configuredPublicBaseUrl.trim().replaceAll("/+$", "");
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String scheme = forwardedProto == null || forwardedProto.isBlank()
                ? request.getScheme() : forwardedProto.split(",")[0].trim();
        String host = forwardedHost == null || forwardedHost.isBlank()
                ? request.getHeader("Host") : forwardedHost.split(",")[0].trim();
        return scheme + "://" + host + request.getContextPath();
    }
}
