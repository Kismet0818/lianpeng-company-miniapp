package com.example.lpsx.service.impl;

import com.example.lpsx.entity.MaterialDownloadRecord;
import com.example.lpsx.entity.User;
import com.example.lpsx.entity.ZhongkaoResourceFile;
import com.example.lpsx.entity.GuideArticle;
import com.example.lpsx.common.exception.BusinessException;
import com.example.lpsx.mapper.GuideArticleMapper;
import com.example.lpsx.mapper.GuideAttachmentMapper;
import com.example.lpsx.vo.GuideAttachmentVO;
import com.example.lpsx.mapper.MaterialDownloadRecordMapper;
import com.example.lpsx.mapper.UserMapper;
import com.example.lpsx.mapper.ZhongkaoResourceFileMapper;
import com.example.lpsx.service.MaterialDownloadRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import org.springframework.util.StringUtils;

@Service
public class MaterialDownloadRecordServiceImpl implements MaterialDownloadRecordService {
    @Resource private MaterialDownloadRecordMapper recordMapper;
    @Resource private UserMapper userMapper;
    @Resource private ZhongkaoResourceFileMapper resourceMapper;
    @Resource private GuideArticleMapper guideArticleMapper;
    @Resource private GuideAttachmentMapper guideAttachmentMapper;

    @Override
    public void record(Integer userId, long resourceId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        ZhongkaoResourceFile file = resourceMapper.selectById(resourceId);
        if (user == null || file == null) throw new IllegalStateException("下载用户或资料不存在");

        MaterialDownloadRecord record = new MaterialDownloadRecord();
        record.setUserId(userId);
        record.setOpenid(user.getOpenid() == null ? "" : user.getOpenid());
        record.setResourceId(resourceId);
        record.setSourceType("SCHOOL_MATERIAL");
        record.setResourceCode(file.getResourceCode());
        record.setResourceTitle(file.getTitle());
        record.setFileName(file.getOriginalFileName());
        record.setDownloadTime(LocalDateTime.now());
        recordMapper.insert(record);
    }

    @Override
    public void recordGuideSave(Integer userId, int articleId, Long attachmentId, String fileUrl, String fileName) {
        User user = userId == null ? null : userMapper.selectById(userId);
        GuideArticle article = articleId > 0 ? guideArticleMapper.selectById(articleId) : null;
        if (user == null) throw new BusinessException(401, "登录状态已失效，请重新登录");
        if (article == null) throw new BusinessException(404, "升学路径资料不存在");

        String verifiedUrl;
        String verifiedName;
        Long verifiedAttachmentId = null;
        if (attachmentId != null) {
            GuideAttachmentVO attachment = guideAttachmentMapper.selectOwnedAttachment(articleId, attachmentId);
            if (attachment == null) throw new BusinessException(400, "附件不属于该升学路径资料");
            verifiedAttachmentId = attachment.getId();
            verifiedUrl = attachment.getFileUrl();
            verifiedName = attachment.getFileName();
        } else {
            verifiedUrl = normalize(fileUrl, 1000, "资料地址不正确");
            String content = article.getContent() == null ? "" : article.getContent();
            boolean owned = verifiedUrl.equals(article.getThumbnailUrl()) || content.contains(verifiedUrl);
            if (!owned) throw new BusinessException(400, "图片不属于该升学路径资料");
            verifiedName = StringUtils.hasText(fileName) ? normalize(fileName, 255, "文件名不正确") : fileNameFromUrl(verifiedUrl);
        }

        MaterialDownloadRecord record = new MaterialDownloadRecord();
        record.setUserId(userId);
        record.setOpenid(user.getOpenid() == null ? "" : user.getOpenid());
        record.setResourceId((long) articleId);
        record.setSourceType("GUIDE");
        record.setSourceAttachmentId(verifiedAttachmentId);
        record.setResourceCode("GUIDE:" + articleId);
        record.setResourceTitle(article.getTitle());
        record.setFileName(verifiedName);
        record.setFileUrl(verifiedUrl);
        record.setDownloadTime(LocalDateTime.now());
        recordMapper.insert(record);
    }

    private String normalize(String value, int max, String message) {
        if (!StringUtils.hasText(value)) throw new BusinessException(message);
        String result = value.trim();
        if (result.length() > max) throw new BusinessException(message);
        return result;
    }

    private String fileNameFromUrl(String url) {
        String clean = url.split("[?#]", 2)[0];
        int slash = clean.lastIndexOf('/');
        String name = slash >= 0 ? clean.substring(slash + 1) : clean;
        return name.isBlank() ? "升学路径图片" : name.substring(0, Math.min(name.length(), 255));
    }
}
