package com.example.lpsx.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.OpenDay;
import com.example.lpsx.entity.School;
import com.example.lpsx.mapper.OpenDayMapper;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.service.OpenDayService;
import com.example.lpsx.vo.OpenDayVO;

import jakarta.annotation.Resource;

/**
 * 学校开放日服务实现
 */
@Service
public class OpenDayServiceImpl implements OpenDayService {

    @Resource
    private OpenDayMapper openDayMapper;

    @Resource
    private SchoolMapper schoolMapper;

    @Override
    public List<OpenDayVO> list() {
        List<OpenDay> openDays = openDayMapper.selectList(
                new LambdaQueryWrapper<OpenDay>()
                        .eq(OpenDay::getIsActive, 1)
                        .orderByDesc(OpenDay::getId));
        return enrich(openDays);
    }

    @Override
    public OpenDayVO detail(int id) {
        OpenDay openDay = openDayMapper.selectById(id);
        if (openDay == null) {
            return null;
        }
        List<OpenDayVO> vos = enrich(List.of(openDay));
        return vos.isEmpty() ? null : vos.get(0);
    }

    /** 批量联查 school 表补充 Logo */
    private List<OpenDayVO> enrich(List<OpenDay> openDays) {
        List<Integer> schoolIds = openDays.stream()
                .map(OpenDay::getSchoolId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> logoMap = schoolIds.isEmpty() ? Map.of()
                : schoolMapper.selectBatchIds(schoolIds).stream()
                        .collect(Collectors.toMap(School::getId, School::getLogoUrl, (a, b) -> a));

        return openDays.stream().map(od -> {
            OpenDayVO vo = new OpenDayVO();
            vo.setId(od.getId());
            vo.setSchoolId(od.getSchoolId());
            vo.setSchoolName(od.getSchoolName());
            vo.setEducationalStages(od.getEducationalStages());
            vo.setCurriculum(od.getCurriculum());
            vo.setAddress(od.getAddress());
            vo.setOpenTime(od.getOpenTime());
            vo.setDescription(od.getDescription());
            vo.setLogoUrl(logoMap.get(od.getSchoolId()));
            return vo;
        }).collect(Collectors.toList());
    }
}
