package com.example.lpsx.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 阿里云 OSS 对象存储服务
 */
public interface OssService {

    /**
     * 上传文件到指定目录
     *
     * @param file   文件
     * @param folder 目录名（如 logo、gallery、qr、guide、banner）
     * @return 文件公网访问 URL
     */
    String upload(MultipartFile file, String folder);

    /**
     * 上传文件到根目录
     *
     * @param file 文件
     * @return 文件公网访问 URL
     */
    String upload(MultipartFile file);

    /**
     * 批量上传
     *
     * @param files  文件列表
     * @param folder 目录名
     * @return URL 列表
     */
    List<String> uploadBatch(List<MultipartFile> files, String folder);

    /**
     * 根据文件 URL 删除 OSS 文件
     *
     * @param url 完整的 OSS 文件 URL
     */
    void deleteByUrl(String url);

    /**
     * 根据 objectName 删除 OSS 文件
     *
     * @param objectName OSS 对象名（如 logo/abc.png）
     */
    void delete(String objectName);

    /**
     * 生成前端直传 OSS 的签名信息（POST 表单直传）
     * <p>
     * 返回 host / accessid / policy / signature / dir / expire，
     * 前端据此用 wx.uploadFile 直接上传文件到 OSS，不经过后端。
     *
     * @param dir 存储目录（如 logo、gallery、avatar），需在 policy 中限定前缀
     * @return 签名信息
     */
    Map<String, String> createUploadPolicy(String dir);
}
