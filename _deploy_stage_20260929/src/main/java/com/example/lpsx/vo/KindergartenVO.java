package com.example.lpsx.vo;

import java.math.BigDecimal;
import com.example.lpsx.entity.Kindergarten;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KindergartenVO {
    private Integer id;
    private String name;
    private String shortName;
    private String backgroundImageUrl;
    private String district;
    private BigDecimal tuitionMin;
    private BigDecimal tuitionMax;
    private String schoolType;
    private String kindergartenLevel;
    private String careType;
    private String ageRange;
    private String address;
    private String phone;
    private String description;
    private String officialWebsite;
    private String schedule;
    private String teachingFeatures;
    private BigDecimal campusArea;
    private String schoolSource;
    private String schoolLevel;

    public static KindergartenVO from(Kindergarten school) {
        return new KindergartenVO(school.getId(), school.getName(), school.getShortName(),
                school.getBackgroundImageUrl(), school.getDistrict(), school.getTuitionMin(),
                school.getTuitionMax(), school.getSchoolType(), school.getKindergartenLevel(),
                school.getCareType(), school.getAgeRange(), school.getAddress(), school.getPhone(),
                school.getDescription(), school.getOfficialWebsite(), school.getSchedule(),
                school.getTeachingFeatures(), school.getCampusArea(), "kindergarten", "幼儿园");
    }
}
