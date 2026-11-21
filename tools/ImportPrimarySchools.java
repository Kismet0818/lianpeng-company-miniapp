import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
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

/** 将小学列表接口 JSON 幂等导入本地 primary_school；默认预览，追加 --apply 才写库。 */
public final class ImportPrimarySchools {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern YAML_VALUE = Pattern.compile("(?m)^\\s*(url|username|password):\\s*([^\\r\\n#]+)");
    private static final String UPSERT = """
            INSERT INTO primary_school
              (school_code,source_school_id,name,short_name,background_image_url,district,
               tuition_min,tuition_max,boarding_info,school_type,address,description,
               official_website,schedule,teaching_features,campus_area)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
              name=VALUES(name),short_name=VALUES(short_name),background_image_url=VALUES(background_image_url),
              district=VALUES(district),tuition_min=VALUES(tuition_min),tuition_max=VALUES(tuition_max),
              boarding_info=VALUES(boarding_info),school_type=VALUES(school_type),address=VALUES(address),
              description=VALUES(description),official_website=VALUES(official_website),schedule=VALUES(schedule),
              teaching_features=VALUES(teaching_features),campus_area=VALUES(campus_area)
            """;

    record School(int sourceId, String name, String shortName, String imageUrl, String district,
                  BigDecimal tuition, String boarding, String type, String address, String description,
                  String website, String schedule, String features, BigDecimal campusArea) {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Usage: ImportPrimarySchools <list.json> [--apply]");
        boolean apply = List.of(args).contains("--apply");
        List<School> schools = read(Path.of(args[0]));
        if (schools.isEmpty()) throw new IllegalStateException("没有小学数据");

        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        Map<String, String> db = database(yaml);
        try (Connection connection = DriverManager.getConnection(db.get("url"), db.get("username"), db.get("password"))) {
            connection.setAutoCommit(false);
            Map<Integer, String> existingCodes = existingCodes(connection);
            int nextCode = nextCode(connection);
            int inserted = 0;
            int updated = 0;
            try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                for (School school : schools) {
                    String code = existingCodes.get(school.sourceId());
                    if (code == null) {
                        code = "p%03d".formatted(nextCode++);
                        inserted++;
                    } else {
                        updated++;
                    }
                    bind(statement, code, school);
                    statement.addBatch();
                }
                if (apply) statement.executeBatch();
            }
            System.out.printf("mode=%s source=%d new=%d existing=%d firstCode=%s lastCode=%s%n",
                    apply ? "APPLY" : "PREVIEW", schools.size(), inserted, updated,
                    existingCodes.isEmpty() ? "p001" : "preserved", "p%03d".formatted(nextCode - 1));
            if (!apply) {
                connection.rollback();
                System.out.println("PREVIEW ONLY: 数据库未修改");
                return;
            }
            connection.commit();
            verify(connection, schools.size());
        }
    }

    private static List<School> read(Path path) throws Exception {
        JsonNode root = JSON.readTree(Files.readString(path, StandardCharsets.UTF_8));
        if (root.path("code").asInt() != 200) throw new IllegalStateException("来源接口响应失败");
        Map<Integer, School> unique = new LinkedHashMap<>();
        for (JsonNode row : root.path("data").path("records")) {
            if (!"小学".equals(text(row, "level"))) continue;
            int sourceId = row.path("id").asInt(0);
            String name = text(row, "name");
            String district = normalizeDistrict(text(row, "area"));
            if (sourceId <= 0 || name == null || district == null) throw new IllegalStateException("存在缺少必填字段的小学");
            BigDecimal tuition = decimal(row.get("tuition"));
            if (tuition != null) tuition = tuition.multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(10_000));
            BigDecimal area = decimal(row.get("floorArea"));
            if (area != null) area = area.multiply(BigDecimal.valueOf(10_000));
            String nature = text(row, "nature");
            String type = nature != null && nature.contains("民") ? "民办" : "公办";
            String remark = text(row, "remark");
            boolean saysNoBoarding = remark != null && (remark.contains("无宿舍") || remark.contains("不提供寄宿") || remark.contains("走读"));
            String boarding = row.path("dormitoryFlag").asBoolean(false) && !saysNoBoarding ? "有住宿" : "走读";
            String picture = text(row, "schoolPic");
            String imageUrl = picture == null ? null : "https://www.anroader.com/upload/" + picture;
            School school = new School(sourceId, name, text(row, "simplaName"), imageUrl, district,
                    tuition, boarding, type, text(row, "address"), text(row, "schoolProfile"),
                    text(row, "web"), text(row, "dailySchedule"), text(row, "hotProject"), area);
            if (unique.put(sourceId, school) != null) throw new IllegalStateException("来源ID重复: " + sourceId);
        }
        List<School> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparingInt(School::sourceId));
        return result;
    }

    private static void bind(PreparedStatement ps, String code, School s) throws Exception {
        int i = 1;
        ps.setString(i++, code); ps.setInt(i++, s.sourceId()); ps.setString(i++, s.name());
        ps.setString(i++, s.shortName()); ps.setString(i++, s.imageUrl()); ps.setString(i++, s.district());
        ps.setBigDecimal(i++, s.tuition()); ps.setBigDecimal(i++, s.tuition()); ps.setString(i++, s.boarding());
        ps.setString(i++, s.type()); ps.setString(i++, s.address()); ps.setString(i++, s.description());
        ps.setString(i++, s.website()); ps.setString(i++, s.schedule()); ps.setString(i++, s.features());
        ps.setBigDecimal(i, s.campusArea());
    }

    private static Map<Integer, String> existingCodes(Connection c) throws Exception {
        Map<Integer, String> result = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT source_school_id,school_code FROM primary_school");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.put(rs.getInt(1), rs.getString(2));
        }
        return result;
    }

    private static int nextCode(Connection c) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(CAST(SUBSTRING(school_code,2) AS UNSIGNED)),0)+1 FROM primary_school");
             ResultSet rs = ps.executeQuery()) {
            rs.next(); return rs.getInt(1);
        }
    }

    private static void verify(Connection c, int expected) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*),COUNT(DISTINCT school_code),COUNT(DISTINCT source_school_id),MIN(id),MAX(id) FROM primary_school");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) != expected || rs.getInt(2) != expected || rs.getInt(3) != expected)
                throw new IllegalStateException("导入后数量或唯一性校验失败");
            System.out.printf("VERIFIED rows=%d uniqueCodes=%d uniqueSourceIds=%d idRange=%d-%d%n",
                    rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5));
        }
    }

    private static Map<String, String> database(String yaml) {
        Map<String, String> values = new HashMap<>();
        Matcher matcher = YAML_VALUE.matcher(yaml);
        while (matcher.find()) values.putIfAbsent(matcher.group(1), placeholder(matcher.group(2).trim()));
        return values;
    }

    private static String placeholder(String raw) {
        Matcher matcher = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}").matcher(raw);
        if (!matcher.matches()) return raw;
        String env = System.getenv(matcher.group(1));
        return env != null && !env.isBlank() ? env : matcher.group(2);
    }

    private static String normalizeDistrict(String value) {
        if ("静安区（原闸北区）".equals(value)) return "静安区";
        if ("浦东新区（本部）".equals(value)) return "浦东新区";
        return value;
    }

    private static BigDecimal decimal(JsonNode value) {
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.decimalValue();
    }

    private static String text(JsonNode row, String field) {
        String value = row.path(field).asText(null);
        if (value == null || value.isBlank()) return null;
        return value.strip();
    }
}
