package com.example.lpsx.service;

public interface MaterialDownloadRecordService {
    void record(Integer userId, long resourceId);
    void recordGuideSave(Integer userId, int articleId, Long attachmentId, String fileUrl, String fileName);
}
