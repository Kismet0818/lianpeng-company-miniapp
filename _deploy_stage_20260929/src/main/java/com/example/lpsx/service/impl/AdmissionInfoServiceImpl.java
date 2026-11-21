package com.example.lpsx.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lpsx.entity.AdmissionInfo;
import com.example.lpsx.mapper.AdmissionInfoMapper;
import com.example.lpsx.service.AdmissionInfoService;

import jakarta.annotation.Resource;

/**
 * 招生动态服务实现
 */
@Service
public class AdmissionInfoServiceImpl implements AdmissionInfoService {

    @Resource
    private AdmissionInfoMapper admissionInfoMapper;

    @Override
    public Map<String, Object> getBySchoolId(Integer schoolId) {
        LambdaQueryWrapper<AdmissionInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdmissionInfo::getSchoolId, schoolId)
               .eq(AdmissionInfo::getIsActive, 1)
               .last("LIMIT 1");
        AdmissionInfo a = admissionInfoMapper.selectOne(wrapper);
        if (a == null) return null;
        return buildStructured(a);
    }

    private Map<String, Object> buildStructured(AdmissionInfo a) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schoolName", a.getSchoolName());

        List<Map<String, Object>> sections = new ArrayList<>();

        // 招生进度
        if (notEmpty(a.getAdmissionEventInfo())) {
            sections.add(section("📅 招生进度", a.getAdmissionEventInfo()));
        }

        // 招生项目
        if (notEmpty(a.getEducationalStages()) || notEmpty(a.getTuitionRange())
                || notEmpty(a.getEnrollmentCount()) || notEmpty(a.getCurriculum())) {
            List<Map<String, Object>> rows = new ArrayList<>();
            addRow(rows, "开设学段", a.getEducationalStages(), false);
            addRow(rows, "每年学费", a.getTuitionRange(), true);
            addRow(rows, "招生人数", a.getEnrollmentCount(), false);
            addRow(rows, "课程体系", a.getCurriculum(), false);
            sections.add(sectionWithRows("📌 招生项目", rows));
        }

        // 考生要求
        if (notEmpty(a.getTargetStudents())) {
            sections.add(section("👤 考生要求", a.getTargetStudents()));
        }

        // 录取要求
        if (notEmpty(a.getAdmissionRules())) {
            sections.add(section("📋 录取要求", a.getAdmissionRules()));
        }

        // 入学测试
        if (notEmpty(a.getExamContent())) {
            sections.add(section("📝 入学测试", a.getExamContent()));
        }

        // 报名流程
        if (notEmpty(a.getApplicationFlow())) {
            sections.add(section("📎 报名流程", a.getApplicationFlow()));
        }

        // 升学成果
        if (notEmpty(a.getGraduationResult())) {
            sections.add(section("🏆 升学成果", a.getGraduationResult()));
        }

        result.put("sections", sections);
        return result;
    }

    private boolean notEmpty(String s) { return s != null && !s.isEmpty(); }

    private Map<String, Object> section(String title, String content) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("title", title);
        s.put("content", content);
        return s;
    }

    private Map<String, Object> sectionWithRows(String title, List<Map<String, Object>> rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("title", title);
        s.put("rows", rows);
        return s;
    }

    private void addRow(List<Map<String, Object>> rows, String label, String value, boolean highlight) {
        if (!notEmpty(value)) return;
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("label", label);
        row.put("value", value);
        if (highlight) row.put("highlight", true);
        rows.add(row);
    }
}
