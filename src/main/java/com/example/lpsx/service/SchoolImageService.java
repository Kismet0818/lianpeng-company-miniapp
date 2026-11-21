package com.example.lpsx.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.SchoolImage;
import com.example.lpsx.mapper.SchoolImageMapper;

import jakarta.annotation.Resource;

/**
 * 学校图片服务
 */
@Service
public class SchoolImageService {

    @Resource
    private SchoolImageMapper schoolImageMapper;

    public List<SchoolImage> getBySchoolId(Integer schoolId) {
        return schoolImageMapper.selectList(
            new LambdaQueryWrapper<SchoolImage>()
                .eq(SchoolImage::getSchoolId, schoolId)
                .orderByAsc(SchoolImage::getSortOrder)
        );
    }
}
