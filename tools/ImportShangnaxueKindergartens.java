import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

/** 将上哪学公开幼儿园快照幂等导入本地 kindergarten；默认预览，追加 --apply 才写库。 */
public final class ImportShangnaxueKindergartens {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern YAML_VALUE = Pattern.compile("(?m)^\\s*(url|username|password):\\s*([^\\r\\n#]+)");
    private static final Pattern FEE = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*(万)?元\\s*/?\\s*(月|年|学期)");
    private static final Pattern AREA = Pattern.compile("(?:占地|户外(?:活动)?(?:场地)?|室内(?:占地)?|建筑面积)[^0-9]{0,12}(\\d+(?:\\.\\d+)?)\\s*(?:平|平方米|㎡)");
    private static final Set<String> SHANGHAI_DISTRICTS = Set.of(
            "黄浦区", "徐汇区", "长宁区", "静安区", "普陀区", "虹口区", "杨浦区", "浦东新区",
            "闵行区", "宝山区", "嘉定区", "金山区", "松江区", "青浦区", "奉贤区", "崇明区");
    private static final String UPSERT = """
            INSERT INTO kindergarten
              (school_code,source_school_id,name,short_name,background_image_url,district,
               tuition_min,tuition_max,school_type,kindergarten_level,care_type,age_range,
               address,phone,description,official_website,teaching_features,campus_area,is_active)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)
            ON DUPLICATE KEY UPDATE
              name=VALUES(name),short_name=VALUES(short_name),background_image_url=VALUES(background_image_url),
              district=VALUES(district),tuition_min=VALUES(tuition_min),tuition_max=VALUES(tuition_max),
              school_type=VALUES(school_type),kindergarten_level=VALUES(kindergarten_level),
              care_type=VALUES(care_type),age_range=VALUES(age_range),address=VALUES(address),phone=VALUES(phone),
              description=VALUES(description),official_website=VALUES(official_website),
              teaching_features=VALUES(teaching_features),campus_area=VALUES(campus_area),is_active=1
            """;

    record School(long sourceId, String name, String shortName, String imageUrl, String district,
                  BigDecimal tuitionMin, BigDecimal tuitionMax, String type, String level,
                  String careType, String ageRange, String address, String phone,
                  String description, String website, String features, BigDecimal campusArea) {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Usage: ImportShangnaxueKindergartens <snapshot.json> [--apply]");
        boolean apply = List.of(args).contains("--apply");
        List<School> schools = read(Path.of(args[0]));
        if (schools.isEmpty()) throw new IllegalStateException("没有幼儿园数据");
        Map<String, String> db = database(Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8));
        try (Connection connection = DriverManager.getConnection(db.get("url"), db.get("username"), db.get("password"))) {
            connection.setAutoCommit(false);
            Map<Long, String> codes = existingCodes(connection);
            int nextCode = nextCode(connection), inserted = 0, updated = 0;
            if (apply) {
                try (var deactivate = connection.prepareStatement("UPDATE kindergarten SET is_active=0")) {
                    deactivate.executeUpdate();
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(UPSERT)) {
                for (School school : schools) {
                    String code = codes.get(school.sourceId());
                    if (code == null) { code = "k%03d".formatted(nextCode++); inserted++; } else updated++;
                    bind(ps, code, school); ps.addBatch();
                }
                if (apply) ps.executeBatch();
            }
            System.out.printf("mode=%s source=%d new=%d existing=%d%n", apply ? "APPLY" : "PREVIEW", schools.size(), inserted, updated);
            if (!apply) { connection.rollback(); System.out.println("PREVIEW ONLY: 数据库未修改"); return; }
            connection.commit(); verify(connection, schools.size());
        }
    }

    private static List<School> read(Path path) throws Exception {
        JsonNode root = JSON.readTree(Files.readString(path, StandardCharsets.UTF_8));
        Map<Long, School> unique = new LinkedHashMap<>();
        for (JsonNode item : root.path("schools")) {
            JsonNode list = item.path("list"), detail = item.path("detail");
            if (!"幼儿园".equals(text(detail, "segmentGradeName"))
                    || !"上海市".equals(text(detail, "cityName"))) continue;
            long sourceId = detail.path("schoolId").asLong(0);
            String name = text(detail, "name"), district = normalizeDistrict(text(detail, "areaName"));
            if (sourceId <= 0 || name == null || district == null || !SHANGHAI_DISTRICTS.contains(district))
                throw new IllegalStateException("存在缺少必填字段或非上海行政区的幼儿园: " + name);
            BigDecimal[] tuition = tuition(text(detail, "feesStandard"));
            String nature = text(detail, "natureName");
            if (!"公办".equals(nature) && !"民办".equals(nature)) continue;
            String type = nature;
            String description = text(detail, "characteristic");
            School school = new School(sourceId, name, limit(text(detail, "abbreviation"), 100),
                    first(text(detail, "schoolPic"), text(list, "schoolPic")), district,
                    tuition[0], tuition[1], type, text(detail, "ratingName"),
                    text(detail, "serviceTypeName"), first(text(detail, "ageRange"), text(list, "ageRange")),
                    text(detail, "address"), text(detail, "phone"), description,
                    text(detail, "officialUrl"), text(detail, "foundedFeatures"), area(description));
            if (unique.put(sourceId, school) != null) throw new IllegalStateException("来源ID重复: " + sourceId);
        }
        List<School> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparingLong(School::sourceId)); return result;
    }

    private static BigDecimal[] tuition(String value) {
        if (value == null) return new BigDecimal[]{null, null};
        Matcher matcher = FEE.matcher(value); BigDecimal min = null, max = null;
        while (matcher.find()) {
            BigDecimal amount = new BigDecimal(matcher.group(1));
            if (matcher.group(2) == null) amount = amount.divide(BigDecimal.valueOf(10_000));
            amount = switch (matcher.group(3)) {
                case "月" -> amount.multiply(BigDecimal.valueOf(12));
                case "学期" -> amount.multiply(BigDecimal.valueOf(2));
                default -> amount;
            };
            amount = amount.setScale(2, RoundingMode.HALF_UP);
            min = min == null || amount.compareTo(min) < 0 ? amount : min;
            max = max == null || amount.compareTo(max) > 0 ? amount : max;
        }
        return new BigDecimal[]{min, max};
    }

    private static BigDecimal area(String value) {
        if (value == null) return null; Matcher matcher = AREA.matcher(value);
        BigDecimal max = null; while (matcher.find()) { BigDecimal n = new BigDecimal(matcher.group(1)); if (max == null || n.compareTo(max) > 0) max = n; }
        return max;
    }

    private static void bind(PreparedStatement ps, String code, School s) throws Exception {
        int i=1; ps.setString(i++,code); ps.setLong(i++,s.sourceId()); ps.setString(i++,s.name()); ps.setString(i++,s.shortName());
        ps.setString(i++,s.imageUrl()); ps.setString(i++,s.district()); ps.setBigDecimal(i++,s.tuitionMin()); ps.setBigDecimal(i++,s.tuitionMax());
        ps.setString(i++,s.type()); ps.setString(i++,s.level()); ps.setString(i++,s.careType()); ps.setString(i++,s.ageRange());
        ps.setString(i++,s.address()); ps.setString(i++,s.phone()); ps.setString(i++,s.description()); ps.setString(i++,s.website());
        ps.setString(i++,s.features()); ps.setBigDecimal(i,s.campusArea());
    }

    private static Map<Long,String> existingCodes(Connection c)throws Exception{Map<Long,String> r=new HashMap<>();try(var ps=c.prepareStatement("SELECT source_school_id,school_code FROM kindergarten");var rs=ps.executeQuery()){while(rs.next())r.put(rs.getLong(1),rs.getString(2));}return r;}
    private static int nextCode(Connection c)throws Exception{try(var ps=c.prepareStatement("SELECT COALESCE(MAX(CAST(SUBSTRING(school_code,2) AS UNSIGNED)),0)+1 FROM kindergarten");var rs=ps.executeQuery()){rs.next();return rs.getInt(1);}}
    private static void verify(Connection c,int expected)throws Exception{try(var ps=c.prepareStatement("SELECT COUNT(*),COUNT(DISTINCT school_code),COUNT(DISTINCT source_school_id),MIN(id),MAX(id) FROM kindergarten WHERE is_active=1");var rs=ps.executeQuery()){rs.next();if(rs.getInt(1)!=expected||rs.getInt(2)!=expected||rs.getInt(3)!=expected)throw new IllegalStateException("导入后有效数据数量或唯一性校验失败");System.out.printf("VERIFIED activeRows=%d uniqueCodes=%d uniqueSourceIds=%d idRange=%d-%d%n",rs.getInt(1),rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getInt(5));}}
    private static Map<String,String> database(String yaml){Map<String,String> v=new HashMap<>();Matcher m=YAML_VALUE.matcher(yaml);while(m.find())v.putIfAbsent(m.group(1),placeholder(m.group(2).trim()));return v;}
    private static String placeholder(String raw){Matcher m=Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}").matcher(raw);if(!m.matches())return raw;String e=System.getenv(m.group(1));return e!=null&&!e.isBlank()?e:m.group(2);}
    private static String normalizeDistrict(String s){if(s==null)return null;s=s.strip();if("浦东".equals(s))return "浦东新区";if(!s.endsWith("区"))s += "区";return s;}
    private static String text(JsonNode n,String f){String v=n.path(f).asText(null);return v==null||v.isBlank()?null:v.strip();}
    private static String first(String a,String b){return a!=null?a:b;}
    private static String limit(String s,int n){return s==null?null:s.substring(0,Math.min(s.length(),n));}
}
