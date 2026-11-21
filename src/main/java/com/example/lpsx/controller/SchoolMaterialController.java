package com.example.lpsx.controller;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.service.SchoolMaterialService;
import com.example.lpsx.service.MaterialDownloadRecordService;
import com.example.lpsx.vo.SchoolMaterialDetailVO;
import com.example.lpsx.vo.SchoolMaterialPageVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/school-materials")
public class SchoolMaterialController {
    @Resource private SchoolMaterialService service;
    @Resource private MaterialDownloadRecordService downloadRecordService;
    @Value("${app.material-public-base-url:}") private String configuredPublicBaseUrl;

    @GetMapping
    public Result<SchoolMaterialPageVO> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String resourceCategory,
                                             @RequestParam(required = false) String subjectCategory,
                                             @RequestParam(required = false) String resourceType,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String year) {
        return Result.success(service.list(Math.max(page, 1), Math.min(Math.max(size, 1), 50),
                resourceCategory, subjectCategory, resourceType, keyword, year));
    }

    @GetMapping("/{id}")
    public Result<SchoolMaterialDetailVO> detail(@PathVariable long id, HttpServletRequest request) {
        SchoolMaterialDetailVO material = id > 0 ? service.detail(id, publicBaseUrl(request)) : null;
        return material == null ? Result.error(404, "资料不存在或已下架") : Result.success(material);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable long id,
                                           @RequestAttribute("userId") Integer userId,
                                           HttpServletRequest request) {
        SchoolMaterialService.DownloadFile file = id > 0 ? service.download(id) : null;
        if (file == null) return ResponseEntity.notFound().build();
        downloadRecordService.record(userId, id);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (Exception ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff").body(file.content());
    }

    private String publicBaseUrl(HttpServletRequest request) {
        if (configuredPublicBaseUrl != null && !configuredPublicBaseUrl.isBlank()) return configuredPublicBaseUrl.trim();
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String scheme = forwardedProto == null || forwardedProto.isBlank() ? request.getScheme() : forwardedProto.split(",")[0].trim();
        String host = forwardedHost == null || forwardedHost.isBlank() ? request.getHeader("Host") : forwardedHost.split(",")[0].trim();
        return scheme + "://" + host + request.getContextPath();
    }
}
