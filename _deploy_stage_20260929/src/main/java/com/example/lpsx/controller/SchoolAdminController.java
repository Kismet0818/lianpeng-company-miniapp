package com.example.lpsx.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.lpsx.common.result.Result;
import com.example.lpsx.entity.School;
import com.example.lpsx.entity.SchoolImage;
import com.example.lpsx.mapper.SchoolImageMapper;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.service.OssService;

import jakarta.annotation.Resource;

/**
 * 学校管理接口（后端管理员用）
 */
@RestController
@RequestMapping("/api/admin/schools")
public class SchoolAdminController {

    @Resource
    private OssService ossService;

    @Resource
    private SchoolMapper schoolMapper;

    @Resource
    private SchoolImageMapper schoolImageMapper;

    /**
     * 上传校徽 → OSS → 自动写入数据库
     *
     * 用法: curl -X POST http://192.168.1.165:8080/api/admin/schools/1/upload-logo -F "file=@logo.png"
     */
    @PostMapping("/{id}/upload-logo")
    public Result<String> uploadLogo(@PathVariable int id,
                                     @RequestParam("file") MultipartFile file) {
        // 上传到 OSS
        String url = ossService.upload(file, "logo");

        // 写入数据库
        School school = schoolMapper.selectById(id);
        if (school == null) {
            return Result.error("学校不存在");
        }
        school.setLogoUrl(url);
        schoolMapper.updateById(school);

        return Result.success(url);
    }

    /**
     * 上传学校详情图 → OSS → 自动写入 school_image 表
     *
     * 用法: curl.exe -X POST http://192.168.1.165:8080/api/admin/schools/12/upload-image -F "file=@yb1.png"
     */
    @PostMapping("/{id}/upload-image")
    public Result<String> uploadImage(@PathVariable int id,
                                      @RequestParam("file") MultipartFile file) {
        String url = ossService.upload(file, "school-gallery/" + id);
        SchoolImage img = new SchoolImage();
        img.setSchoolId(id);
        img.setImageUrl(url);
        img.setImageType(1);
        img.setSortOrder(0);
        schoolImageMapper.insert(img);
        return Result.success(url);
    }
}
