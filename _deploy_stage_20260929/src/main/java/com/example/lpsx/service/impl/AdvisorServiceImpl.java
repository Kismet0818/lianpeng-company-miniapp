package com.example.lpsx.service.impl;

import com.example.lpsx.entity.GeneralHighSchool;
import com.example.lpsx.entity.JuniorHighSchool;
import com.example.lpsx.entity.PrimarySchool;
import com.example.lpsx.entity.School;
import com.example.lpsx.entity.ScoreQueryToolSchool;
import com.example.lpsx.mapper.GeneralHighSchoolMapper;
import com.example.lpsx.mapper.JuniorHighSchoolMapper;
import com.example.lpsx.mapper.PrimarySchoolMapper;
import com.example.lpsx.mapper.SchoolMapper;
import com.example.lpsx.mapper.ScoreQueryToolSchoolMapper;
import com.example.lpsx.service.AdvisorService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AdvisorServiceImpl implements AdvisorService {
    private static final List<String> DISTRICTS = List.of(
            "浦东新区", "黄浦区", "徐汇区", "长宁区", "静安区", "普陀区", "虹口区", "杨浦区",
            "闵行区", "宝山区", "嘉定区", "金山区", "松江区", "青浦区", "奉贤区", "崇明区");

    @Resource private SchoolMapper schoolMapper;
    @Resource private PrimarySchoolMapper primaryMapper;
    @Resource private JuniorHighSchoolMapper juniorMapper;
    @Resource private GeneralHighSchoolMapper generalMapper;
    @Resource private ScoreQueryToolSchoolMapper scoreMapper;

    @Override
    public Map<String, Object> welcome() {
        return response("stage", "你好，我是联朋智能升学顾问。先告诉我，孩子准备选择哪个学段？",
                Map.of(), choices("小学", "小学", "初中", "初中", "高中", "高中"), List.of(), false);
    }

    @Override
    public Map<String, Object> reply(String message, Map<String, Object> oldContext) {
        Map<String, Object> context = sanitize(oldContext);
        String text = message == null ? "" : message.trim();
        if (text.length() > 200) text = text.substring(0, 200);
        if (containsAny(text, "人工", "老师", "电话", "联系", "咨询")) {
            return response("lead", "可以，留下联系方式后，升学老师会继续协助你。",
                    context, List.of(choice("联系升学老师", "lead")), List.of(), true);
        }

        capture(text, context);
        String missing = firstMissing(context);
        if (missing != null) return question(missing, context);

        List<Map<String, Object>> schools = recommend(context);
        String reply = schools.isEmpty()
                ? "暂时没有完全符合这些条件的学校。你可以把区域或预算改成“不限”，我再帮你找。"
                : "我根据学段、体制、区域、预算和寄宿需求筛出了这些学校。推荐仅用于初步筛选，最终请以学校最新招生政策为准。";
        return response("result", reply, context,
                List.of(choice("重新选择", "restart"), choice("联系升学老师", "lead")), schools, true);
    }

    private void capture(String text, Map<String, Object> context) {
        if (text.isBlank()) return;
        if (containsAny(text, "重新", "重来", "清空", "开始")) { context.clear(); return; }
        if (containsAny(text, "小学")) context.put("stage", "小学");
        else if (containsAny(text, "初中", "小升初")) context.put("stage", "初中");
        else if (containsAny(text, "高中", "中考")) context.put("stage", "高中");

        if (containsAny(text, "体制外", "国际", "双语")) context.put("educationSystem", "体制外");
        else if (containsAny(text, "体制内", "公办", "民办")) context.put("educationSystem", "体制内");

        for (String district : DISTRICTS) {
            if (text.contains(district) || text.contains(district.replace("区", ""))) {
                context.put("district", district); break;
            }
        }
        if (containsAny(text, "全上海", "区域不限", "地区不限", "不限区域")) context.put("district", "不限");

        if (containsAny(text, "预算不限", "学费不限", "不限预算")) context.put("budget", "不限");
        else {
            Integer budget = firstNumber(text);
            if (budget != null && containsAny(text, "万", "预算", "学费")) context.put("budget", Math.min(budget, 200));
        }

        if (containsAny(text, "不寄宿", "走读")) context.put("boarding", "不寄宿");
        else if (containsAny(text, "要寄宿", "需要寄宿", "住宿", "寄宿")) context.put("boarding", "寄宿");
        else if (containsAny(text, "寄宿不限", "都可以")) context.put("boarding", "不限");
    }

    private String firstMissing(Map<String, Object> context) {
        for (String key : List.of("stage", "educationSystem", "district", "budget", "boarding"))
            if (!context.containsKey(key)) return key;
        return null;
    }

    private Map<String, Object> question(String key, Map<String, Object> context) {
        return switch (key) {
            case "stage" -> response(key, "孩子准备选择哪个学段？", context,
                    choices("小学", "小学", "初中", "初中", "高中", "高中"), List.of(), false);
            case "educationSystem" -> response(key, "主要考虑体制内还是体制外学校？", context,
                    choices("体制内", "体制内", "体制外", "体制外"), List.of(), false);
            case "district" -> response(key, "优先考虑哪个行政区？也可以选择全上海。", context,
                    choices("浦东新区", "浦东新区", "徐汇区", "徐汇区", "闵行区", "闵行区", "全上海", "不限区域"), List.of(), false);
            case "budget" -> response(key, "每年学费预算上限是多少万元？", context,
                    choices("10万内", "10万", "20万内", "20万", "30万内", "30万", "不限", "预算不限"), List.of(), false);
            default -> response(key, "是否需要寄宿？", context,
                    choices("需要寄宿", "需要寄宿", "走读", "走读", "都可以", "寄宿不限"), List.of(), false);
        };
    }

    private List<Map<String, Object>> recommend(Map<String, Object> context) {
        String stage = String.valueOf(context.get("stage"));
        String system = String.valueOf(context.get("educationSystem"));
        List<Card> candidates = new ArrayList<>();
        if ("体制外".equals(system)) {
            for (School s : schoolMapper.selectList(null)) {
                if (gradeMatches(s.getGrades(), stage)) candidates.add(new Card(s.getId(), "international", s.getName(),
                        s.getDistrict(), s.getLogoUrl(), s.getSchoolType(), s.getTuitionMin(), s.getTuitionMax(), s.getBoardingInfo()));
            }
        } else if ("小学".equals(stage)) {
            for (PrimarySchool s : primaryMapper.selectList(null)) candidates.add(new Card(s.getId(), "primary", s.getName(),
                    s.getDistrict(), s.getBackgroundImageUrl(), s.getSchoolType(), s.getTuitionMin(), s.getTuitionMax(), s.getBoardingInfo()));
        } else if ("初中".equals(stage)) {
            for (JuniorHighSchool s : juniorMapper.selectList(null)) candidates.add(new Card(s.getId(), "junior", s.getName(),
                    s.getDistrict(), s.getBackgroundImageUrl(), s.getSchoolType(), s.getTuitionMin(), s.getTuitionMax(), s.getBoardingInfo()));
        } else {
            for (ScoreQueryToolSchool s : scoreMapper.selectList(null)) candidates.add(new Card(s.getId(), "score", s.getName(),
                    s.getDistrict(), s.getLogo(), s.getSchoolType(), s.getTuitionMin(), s.getTuitionMax(), s.getBoardingInfo()));
            for (GeneralHighSchool s : generalMapper.selectList(null)) candidates.add(new Card(s.getId(), "general", s.getName(),
                    s.getDistrict(), s.getBackgroundImageUrl(), s.getSchoolType(), s.getTuitionMin(), s.getTuitionMax(), s.getBoardingInfo()));
        }

        String district = String.valueOf(context.get("district"));
        Object rawBudget = context.get("budget");
        BigDecimal budget = rawBudget instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : null;
        String boarding = String.valueOf(context.get("boarding"));
        Set<String> names = new LinkedHashSet<>();
        return candidates.stream()
                .filter(s -> "不限".equals(district) || district.equals(s.district))
                .filter(s -> budget == null || s.tuitionMax == null || s.tuitionMax.compareTo(budget) <= 0)
                .filter(s -> !"寄宿".equals(boarding) || containsAny(s.boarding, "寄宿", "住宿", "提供"))
                .filter(s -> names.add(s.name))
                .sorted(Comparator.comparing((Card s) -> s.imageUrl == null || s.imageUrl.isBlank()).thenComparing(s -> s.name))
                .limit(6).map(this::cardMap).toList();
    }

    private Map<String, Object> cardMap(Card s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.id); map.put("schoolId", s.id); map.put("schoolSource", s.source);
        map.put("name", s.name); map.put("district", s.district); map.put("logoUrl", s.imageUrl);
        map.put("schoolType", s.schoolType); map.put("tuitionMin", s.tuitionMin); map.put("tuitionMax", s.tuitionMax);
        map.put("boardingInfo", s.boarding); return map;
    }

    private Map<String, Object> sanitize(Map<String, Object> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (raw == null) return result;
        for (String key : List.of("stage", "educationSystem", "district", "budget", "boarding")) {
            Object value = raw.get(key);
            if (value != null && String.valueOf(value).length() <= 30) result.put(key, value);
        }
        return result;
    }

    private Map<String, Object> response(String step, String reply, Map<String, Object> context,
                                         List<Map<String, String>> choices, List<Map<String, Object>> schools, boolean canContact) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("step", step); map.put("reply", reply); map.put("context", new LinkedHashMap<>(context));
        map.put("choices", choices); map.put("schools", schools); map.put("canContact", canContact);
        map.put("engine", "rule-based"); map.put("modelCost", 0); return map;
    }

    private List<Map<String, String>> choices(String... pairs) {
        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) list.add(choice(pairs[i], pairs[i + 1]));
        return list;
    }
    private Map<String, String> choice(String label, String value) { return Map.of("label", label, "value", value); }
    private boolean gradeMatches(String grades, String stage) {
        if (grades == null) return false;
        return switch (stage) {
            case "小学" -> containsAny(grades, "小学", "幼小", "1-5", "1-6");
            case "初中" -> containsAny(grades, "初中", "中学", "6-9", "7-9");
            default -> containsAny(grades, "高中", "10-12", "9-12");
        };
    }
    private boolean containsAny(String text, String... values) {
        if (text == null) return false;
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String value : values) if (normalized.contains(value.toLowerCase(Locale.ROOT))) return true;
        return false;
    }
    private Integer firstNumber(String text) {
        String digits = text.replaceAll(".*?(\\d{1,3}).*", "$1");
        try { return digits.equals(text) && !text.matches(".*\\d.*") ? null : Integer.valueOf(digits); }
        catch (Exception ignored) { return null; }
    }
    private record Card(Integer id, String source, String name, String district, String imageUrl,
                        String schoolType, BigDecimal tuitionMin, BigDecimal tuitionMax, String boarding) {}
}
