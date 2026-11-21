package com.example.lpsx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lpsx.entity.ZhongkaoResourceFile;
import com.example.lpsx.mapper.ZhongkaoResourceFileMapper;
import com.example.lpsx.service.SchoolMaterialService;
import com.example.lpsx.service.PdfPreviewPageService;
import com.example.lpsx.vo.SchoolMaterialAttachmentVO;
import com.example.lpsx.vo.SchoolMaterialDetailVO;
import com.example.lpsx.vo.SchoolMaterialListVO;
import com.example.lpsx.vo.SchoolMaterialPageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Service
public class SchoolMaterialServiceImpl implements SchoolMaterialService {
    @Resource private ZhongkaoResourceFileMapper mapper;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private PdfPreviewPageService pdfPreviewPageService;

    @Override
    public SchoolMaterialPageVO list(int page, int size, String resourceCategory, String subjectCategory,
                                     String resourceType, String keyword, String year) {
        LambdaQueryWrapper<ZhongkaoResourceFile> query = new LambdaQueryWrapper<ZhongkaoResourceFile>()
                .select(ZhongkaoResourceFile::getId, ZhongkaoResourceFile::getResourceCode,
                        ZhongkaoResourceFile::getTitle, ZhongkaoResourceFile::getResourceCategory,
                        ZhongkaoResourceFile::getSubjectCategory, ZhongkaoResourceFile::getResourceType,
                        ZhongkaoResourceFile::getTargetYear, ZhongkaoResourceFile::getSourceYear,
                        ZhongkaoResourceFile::getFileExtension, ZhongkaoResourceFile::getFileSize,
                        ZhongkaoResourceFile::getSortOrder)
                .eq(ZhongkaoResourceFile::getStatus, 1).eq(ZhongkaoResourceFile::getIsDeleted, 0)
                .eq(StringUtils.hasText(resourceCategory), ZhongkaoResourceFile::getResourceCategory, trim(resourceCategory))
                .eq(StringUtils.hasText(subjectCategory), ZhongkaoResourceFile::getSubjectCategory, trim(subjectCategory))
                .eq(StringUtils.hasText(resourceType), ZhongkaoResourceFile::getResourceType, trim(resourceType));
        if (StringUtils.hasText(keyword)) {
            String value = trim(keyword);
            query.and(q -> q.like(ZhongkaoResourceFile::getTitle, value)
                    .or().like(ZhongkaoResourceFile::getResourceCode, value));
        }
        if (StringUtils.hasText(year)) {
            String value = trim(year);
            query.and(q -> q.eq(ZhongkaoResourceFile::getTargetYear, value)
                    .or().eq(ZhongkaoResourceFile::getSourceYear, value));
        }
        query.orderByAsc(ZhongkaoResourceFile::getSortOrder).orderByDesc(ZhongkaoResourceFile::getId);
        Page<ZhongkaoResourceFile> rows = mapper.selectPage(new Page<>(page, size), query);
        return new SchoolMaterialPageVO(rows.getRecords().stream().map(this::toListVO).toList(),
                rows.getTotal(), rows.getCurrent(), rows.getSize());
    }

    @Override
    public SchoolMaterialDetailVO detail(long id, String publicBaseUrl) {
        ZhongkaoResourceFile file = activeFile(id);
        if (file == null) return null;
        SchoolMaterialDetailVO vo = new SchoolMaterialDetailVO();
        vo.setId(file.getId()); vo.setResourceCode(file.getResourceCode()); vo.setTitle(file.getTitle());
        vo.setResourceCategory(file.getResourceCategory()); vo.setSubjectCategory(file.getSubjectCategory());
        vo.setResourceType(file.getResourceType()); vo.setYear(file.getTargetYear());
        vo.setSummary(""); vo.setContent("");
        SchoolMaterialAttachmentVO attachment = new SchoolMaterialAttachmentVO();
        attachment.setId(file.getId()); attachment.setName(file.getOriginalFileName());
        attachment.setUrl(stripTrailingSlash(publicBaseUrl) + "/api/school-materials/" + file.getId() + "/download");
        attachment.setExtension(file.getFileExtension()); attachment.setMimeType(file.getMimeType());
        attachment.setSize(file.getFileSize()); attachment.setSort(1);
        vo.setAttachments(List.of(attachment));
        return vo;
    }

    @Override
    public DownloadFile download(long id) {
        ZhongkaoResourceFile metadata = activeFile(id);
        if (metadata == null) return null;
        byte[] content = jdbcTemplate.queryForObject(
                "SELECT file_content FROM zhongkao_resource_file WHERE id=? AND status=1 AND is_deleted=0",
                (resultSet, rowNum) -> resultSet.getBytes(1), id);
        if (content == null || content.length != metadata.getFileSize()) return null;
        if ("pdf".equalsIgnoreCase(metadata.getFileExtension())
                || "application/pdf".equalsIgnoreCase(metadata.getMimeType())) {
            content = pdfPreviewPageService.appendClaimPage(content);
        }
        return new DownloadFile(metadata.getOriginalFileName(), metadata.getFileExtension(),
                metadata.getMimeType(), content.length, content);
    }

    private ZhongkaoResourceFile activeFile(long id) {
        return mapper.selectOne(new LambdaQueryWrapper<ZhongkaoResourceFile>()
                .eq(ZhongkaoResourceFile::getId, id).eq(ZhongkaoResourceFile::getStatus, 1)
                .eq(ZhongkaoResourceFile::getIsDeleted, 0));
    }

    private SchoolMaterialListVO toListVO(ZhongkaoResourceFile file) {
        SchoolMaterialListVO vo = new SchoolMaterialListVO();
        vo.setId(file.getId()); vo.setResourceCode(file.getResourceCode()); vo.setTitle(file.getTitle());
        vo.setResourceCategory(file.getResourceCategory()); vo.setSubjectCategory(file.getSubjectCategory());
        vo.setResourceType(file.getResourceType()); vo.setSummary(""); vo.setPublishDate("");
        vo.setYear(file.getTargetYear()); vo.setCoverUrl(null); vo.setHasAttachment(true);
        return vo;
    }

    private String trim(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String stripTrailingSlash(String value) { return value == null ? "" : value.replaceAll("/+$", ""); }
}
