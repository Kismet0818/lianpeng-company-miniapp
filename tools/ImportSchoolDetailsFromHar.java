import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从小程序 HAR 的 /api/mini/detail 响应中提取学校官网、作息、教学特色和占地面积。
 *
 * 默认仅预览：
 *   java ... ImportSchoolDetailsFromHar "C:\\claude\\崇明 高中.har"
 * 确认后写库：
 *   java ... ImportSchoolDetailsFromHar "C:\\claude\\崇明 高中.har" --apply
 * 已有非空值默认保留；确需覆盖时追加 --overwrite。
 */
public final class ImportSchoolDetailsFromHar {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> TABLES = List.of("score_query_tool_school", "general_high_school", "junior_high_school");
    private static final Pattern YAML_VALUE = Pattern.compile("(?m)^\\s*(url|username|password):\\s*([^\\r\\n#]+)");

    record SourceSchool(String sourceId, String name, String district, String level, String website,
                        String schedule, String teachingFeatures, BigDecimal campusArea,
                        BigDecimal tuition, String schoolType, String address, String boardingInfo, String description) {}
    record TargetSchool(String table, int id, String name, String district, String website,
                        String schedule, String teachingFeatures, BigDecimal campusArea, BigDecimal tuitionMin,
                        BigDecimal tuitionMax, String schoolType, String address, String boardingInfo, String description) {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException(
                "Usage: ImportSchoolDetailsFromHar <file.har> [--apply] [--overwrite] OR --area <区名> [--apply] [--overwrite]");
        boolean apply = hasArg(args, "--apply");
        boolean overwrite = hasArg(args, "--overwrite");
        boolean allowUnmatched = hasArg(args, "--allow-unmatched");
        String area = optionValue(args, "--area");
        String level = optionValue(args, "--level");
        List<SourceSchool> sources = area == null ? readHar(Path.of(args[0])) : fetchArea(area, level == null ? "高中" : level);
        if (sources.isEmpty()) throw new IllegalStateException("没有获取到有效的学校数据");

        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        Map<String, String> database = readDatabaseConfig(yaml);
        try (Connection connection = DriverManager.getConnection(
                database.get("url"), database.get("username"), database.get("password"))) {
            connection.setAutoCommit(false);
            List<TargetSchool> targets = loadTargets(connection);
            Map<SourceSchool, List<TargetSchool>> matches = match(sources, targets);
            printPreview(matches, apply, overwrite);
            validateMatches(matches, allowUnmatched);

            if (!apply) {
                connection.rollback();
                System.out.println("PREVIEW ONLY: 数据库未修改。确认后追加 --apply。 ");
                return;
            }

            int updated = 0;
            try {
                for (Map.Entry<SourceSchool, List<TargetSchool>> entry : matches.entrySet()) {
                    for (TargetSchool target : entry.getValue()) {
                        updated += update(connection, entry.getKey(), target, overwrite);
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
            verify(connection, matches, overwrite);
            System.out.printf("APPLIED AND VERIFIED: sourceSchools=%d targetRows=%d%n", sources.size(), updated);
        }
    }

    private static List<SourceSchool> readHar(Path path) throws Exception {
        String harText = Files.readString(path, StandardCharsets.UTF_8);
        if (!harText.isEmpty() && harText.charAt(0) == '\uFEFF') harText = harText.substring(1);
        JsonNode root = JSON.readTree(harText);
        Map<String, SourceSchool> schools = new LinkedHashMap<>();
        for (JsonNode entry : root.path("log").path("entries")) {
            String url = entry.path("request").path("url").asText();
            if (!url.contains("/api/mini/detail") && !url.contains("/api/mini/list")) continue;
            JsonNode content = entry.path("response").path("content");
            String text = content.path("text").asText("");
            if ("base64".equalsIgnoreCase(content.path("encoding").asText())) {
                text = new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
            }
            if (text.isBlank()) continue;
            JsonNode data;
            try {
                data = JSON.readTree(text).path("data");
            } catch (Exception ignored) {
                continue;
            }
            if (url.contains("/api/mini/list")) {
                for (JsonNode record : data.path("records")) addSchool(schools, record);
            } else {
                addSchool(schools, data);
            }
        }
        return new ArrayList<>(schools.values());
    }

    private static List<SourceSchool> fetchArea(String area, String level) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
        Map<String, SourceSchool> schools = new LinkedHashMap<>();
        int page = 1;
        int pages;
        do {
            String query = "current=" + page + "&size=100&name=&area=" + encode(area) + "&level=" + encode(level) + "&nature=";
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://www.anroader.com/api/mini/list?" + query))
                    .timeout(java.time.Duration.ofSeconds(20)).header("Accept", "application/json").GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw new IllegalStateException("列表接口 HTTP " + response.statusCode());
            JsonNode root = JSON.readTree(response.body());
            if (root.path("code").asInt() != 200) throw new IllegalStateException("列表接口失败: " + root.path("msg").asText());
            JsonNode data = root.path("data");
            for (JsonNode record : data.path("records")) addSchool(schools, record);
            pages = Math.max(1, data.path("pages").asInt(1));
            page++;
            if (page <= pages) Thread.sleep(300);
        } while (page <= pages);
        return new ArrayList<>(schools.values());
    }

    private static void addSchool(Map<String, SourceSchool> schools, JsonNode data) {
        String id = data.path("id").asText("");
        String name = clean(data.path("name").asText(null));
        String level = clean(data.path("level").asText(null));
        if (id.isBlank() || name == null || !("高中".equals(level) || "初中".equals(level))) return;
        BigDecimal hectares = decimal(data.get("floorArea"));
        BigDecimal squareMeters = hectares == null ? null : hectares.multiply(BigDecimal.valueOf(10_000));
        BigDecimal tuition = decimal(data.get("tuition"));
        // 来源字段为“元/学期”，数据库统一保存为“万元/年”。
        if (tuition != null) tuition = tuition.multiply(BigDecimal.valueOf(2))
                .divide(BigDecimal.valueOf(10_000));
        String boarding = data.path("dormitoryFlag").asBoolean(false) ? "有住宿" : "走读";
        String nature = clean(data.path("nature").asText(null));
        String normalizedType = nature;
        if ("初中".equals(level) && nature != null) normalizedType = nature.contains("民") ? "民办初中" : "公办初中";
        if ("高中".equals(level) && nature != null) normalizedType = nature.contains("民") ? "民办" : "公办";
        SourceSchool incoming = new SourceSchool(id, name, clean(data.path("area").asText(null)), level,
                clean(data.path("web").asText(null)), clean(data.path("dailySchedule").asText(null)),
                clean(data.path("hotProject").asText(null)), squareMeters, tuition,
                normalizedType, clean(data.path("address").asText(null)), boarding,
                clean(data.path("schoolProfile").asText(null)));
        SourceSchool old = schools.get(id);
        schools.put(id, old == null ? incoming : new SourceSchool(id, incoming.name(), first(incoming.district(), old.district()), incoming.level(),
                first(incoming.website(), old.website()), first(incoming.schedule(), old.schedule()),
                first(incoming.teachingFeatures(), old.teachingFeatures()),
                incoming.campusArea() != null ? incoming.campusArea() : old.campusArea(),
                incoming.tuition() != null ? incoming.tuition() : old.tuition(), first(incoming.schoolType(), old.schoolType()),
                first(incoming.address(), old.address()), first(incoming.boardingInfo(), old.boardingInfo()), first(incoming.description(), old.description())));
    }

    private static List<TargetSchool> loadTargets(Connection connection) throws Exception {
        List<TargetSchool> result = new ArrayList<>();
        for (String table : TABLES) {
            String sql = "SELECT id,name,district,official_website,schedule,teaching_features,campus_area,tuition_min,tuition_max,school_type,address,boarding_info,description FROM " + table;
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new TargetSchool(table, rows.getInt("id"), rows.getString("name"), rows.getString("district"),
                            rows.getString("official_website"), rows.getString("schedule"),
                            rows.getString("teaching_features"), rows.getBigDecimal("campus_area"), rows.getBigDecimal("tuition_min"), rows.getBigDecimal("tuition_max"), rows.getString("school_type"), rows.getString("address"), rows.getString("boarding_info"), rows.getString("description")));
                }
            }
        }
        return result;
    }

    private static Map<SourceSchool, List<TargetSchool>> match(List<SourceSchool> sources,
                                                                List<TargetSchool> targets) {
        Map<SourceSchool, List<TargetSchool>> result = new LinkedHashMap<>();
        for (SourceSchool source : sources) {
            List<TargetSchool> found = targets.stream()
                    .filter(target -> source.district() == null || source.district().equals(target.district()))
                    .filter(target -> source.level().equals("初中") ? target.table().equals("junior_high_school") : !target.table().equals("junior_high_school"))
                    .filter(target -> normalizeName(target.name(), source.district()).equals(normalizeName(source.name(), source.district())))
                    .toList();
            result.put(source, found);
        }
        return result;
    }

    private static void validateMatches(Map<SourceSchool, List<TargetSchool>> matches, boolean allowUnmatched) {
        List<String> missing = matches.entrySet().stream().filter(e -> e.getValue().isEmpty())
                .map(e -> e.getKey().name()).toList();
        if (!missing.isEmpty() && !allowUnmatched) throw new IllegalStateException("数据库中未匹配学校: " + missing + "；如需跳过请加 --allow-unmatched");
        if (!missing.isEmpty()) System.out.println("UNMATCHED (skipped): " + missing.size());
        for (Map.Entry<SourceSchool, List<TargetSchool>> entry : matches.entrySet()) {
            long sameTableDuplicates = entry.getValue().stream()
                    .collect(java.util.stream.Collectors.groupingBy(TargetSchool::table, java.util.stream.Collectors.counting()))
                    .values().stream().filter(count -> count > 1).count();
            if (sameTableDuplicates > 0) throw new IllegalStateException("同表匹配到多个学校: " + entry.getKey().name());
        }
    }

    private static void printPreview(Map<SourceSchool, List<TargetSchool>> matches,
                                     boolean apply, boolean overwrite) {
        System.out.printf("mode=%s overwrite=%s%n", apply ? "APPLY" : "PREVIEW", overwrite);
        for (Map.Entry<SourceSchool, List<TargetSchool>> entry : matches.entrySet()) {
            SourceSchool source = entry.getKey();
            String target = entry.getValue().isEmpty() ? "UNMATCHED" : entry.getValue().stream()
                    .map(row -> row.table() + "#" + row.id() + "(" + row.name() + ")")
                    .reduce((a, b) -> a + ", " + b).orElse("");
            System.out.printf("- source#%s %s -> %s | website=%s schedule=%s features=%s area=%s㎡%n",
                    source.sourceId(), source.name(), target, present(source.website()), present(source.schedule()),
                    present(source.teachingFeatures()), source.campusArea() == null ? "NULL" : source.campusArea().toPlainString());
        }
    }

    private static int update(Connection connection, SourceSchool source, TargetSchool target,
                              boolean overwrite) throws Exception {
        String assignment = overwrite ? "?" : "CASE WHEN %s IS NULL OR TRIM(%s)='' THEN ? ELSE %s END";
        String website = overwrite ? assignment : assignment.formatted("official_website", "official_website", "official_website");
        String schedule = overwrite ? assignment : assignment.formatted("schedule", "schedule", "schedule");
        String features = overwrite ? assignment : assignment.formatted("teaching_features", "teaching_features", "teaching_features");
        String area = overwrite ? "?" : "COALESCE(campus_area, ?)";
        String tuitionMin = overwrite ? "?" : "COALESCE(tuition_min, ?)";
        String tuitionMax = overwrite ? "?" : "COALESCE(tuition_max, ?)";
        String type = overwrite ? "?" : assignment.formatted("school_type", "school_type", "school_type");
        String address = overwrite ? "?" : assignment.formatted("address", "address", "address");
        String boarding = overwrite ? "?" : assignment.formatted("boarding_info", "boarding_info", "boarding_info");
        String description = overwrite ? "?" : assignment.formatted("description", "description", "description");
        String sql = "UPDATE " + target.table() + " SET official_website=" + website
                + ",schedule=" + schedule + ",teaching_features=" + features + ",campus_area=" + area
                + ",tuition_min=" + tuitionMin + ",tuition_max=" + tuitionMax + ",school_type=" + type
                + ",address=" + address + ",boarding_info=" + boarding + ",description=" + description + " WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source.website());
            statement.setString(2, source.schedule());
            statement.setString(3, source.teachingFeatures());
            statement.setBigDecimal(4, source.campusArea());
            statement.setBigDecimal(5, source.tuition());
            statement.setBigDecimal(6, source.tuition());
            statement.setString(7, source.schoolType());
            statement.setString(8, source.address());
            statement.setString(9, source.boardingInfo());
            statement.setString(10, source.description());
            statement.setInt(11, target.id());
            return statement.executeUpdate();
        }
    }

    private static void verify(Connection connection, Map<SourceSchool, List<TargetSchool>> matches,
                               boolean overwrite) throws Exception {
        for (Map.Entry<SourceSchool, List<TargetSchool>> entry : matches.entrySet()) {
            for (TargetSchool before : entry.getValue()) {
                String sql = "SELECT official_website,schedule,teaching_features,campus_area FROM " + before.table() + " WHERE id=?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, before.id());
                    try (ResultSet row = statement.executeQuery()) {
                        if (!row.next()) throw new IllegalStateException("写入后记录消失: " + before.table() + "#" + before.id());
                        SourceSchool source = entry.getKey();
                        assertValue("official_website", overwrite || blank(before.website()) ? source.website() : before.website(), row.getString(1));
                        assertValue("schedule", overwrite || blank(before.schedule()) ? source.schedule() : before.schedule(), row.getString(2));
                        assertValue("teaching_features", overwrite || blank(before.teachingFeatures()) ? source.teachingFeatures() : before.teachingFeatures(), row.getString(3));
                        BigDecimal expectedArea = overwrite || before.campusArea() == null ? source.campusArea() : before.campusArea();
                        BigDecimal actualArea = row.getBigDecimal(4);
                        if (expectedArea == null ? actualArea != null : actualArea == null || expectedArea.compareTo(actualArea) != 0) {
                            throw new IllegalStateException("campus_area 验证失败: " + before.table() + "#" + before.id());
                        }
                    }
                }
            }
        }
    }

    private static void assertValue(String field, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(field + " 验证失败");
        }
    }

    private static Map<String, String> readDatabaseConfig(String yaml) {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = YAML_VALUE.matcher(yaml);
        while (matcher.find()) values.putIfAbsent(matcher.group(1), resolvePlaceholder(matcher.group(2).trim()));
        for (String key : List.of("url", "username", "password")) {
            if (!values.containsKey(key)) throw new IllegalStateException("缺少数据库配置: " + key);
        }
        return values;
    }

    private static String resolvePlaceholder(String raw) {
        Matcher placeholder = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}").matcher(raw);
        if (!placeholder.matches()) return raw;
        String environment = System.getenv(placeholder.group(1));
        if (environment != null && !environment.isBlank()) return environment;
        if (placeholder.group(2) != null) return placeholder.group(2);
        throw new IllegalStateException("缺少环境变量: " + placeholder.group(1));
    }

    private static String normalizeName(String value, String district) {
        String normalized = value;
        if (district != null) normalized = normalized.replace(district, "");
        return normalized.replaceAll("\\s+", "").trim();
    }

    private static boolean hasArg(String[] args, String expected) {
        for (String arg : args) if (expected.equalsIgnoreCase(arg)) return true;
        return false;
    }

    private static String optionValue(String[] args, String option) {
        for (int i = 0; i < args.length - 1; i++) if (option.equalsIgnoreCase(args[i])) return args[i + 1];
        return null;
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String first(String preferred, String fallback) { return preferred != null ? preferred : fallback; }

    private static BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull() || node.asText().isBlank()) return null;
        return node.decimalValue();
    }

    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String present(String value) { return value == null ? "NULL" : "YES"; }
}
