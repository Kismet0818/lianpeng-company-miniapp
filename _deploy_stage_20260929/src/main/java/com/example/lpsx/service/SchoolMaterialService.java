package com.example.lpsx.service;

import com.example.lpsx.vo.SchoolMaterialDetailVO;
import com.example.lpsx.vo.SchoolMaterialPageVO;

public interface SchoolMaterialService {
    SchoolMaterialPageVO list(int page, int size, String resourceCategory, String subjectCategory,
                              String resourceType, String keyword, String year);
    SchoolMaterialDetailVO detail(long id, String publicBaseUrl);
    DownloadFile download(long id);
    record DownloadFile(String fileName, String extension, String mimeType, long size, byte[] content) {}
}
