import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InsertWuaSeniorHighSchool {
    private static final String NAME = "上海市五爱高级中学";
    private static final String SHORT_NAME = "五爱高级";
    private static final String DISTRICT = "黄浦区";
    private static final String SCHOOL_TYPE = "区实验性示范性高中（区重点）";
    private static final String ADDRESS = "上海市黄浦区制造局路666号";
    private static final String DESCRIPTION = """
            2026年停止招生。

            学校占地面积约38亩，环境优美，亭台廊榭，花草树木，一派园林化校园景象，是上海市“花园单位”。学校设行政、教育、实验、综合、艺术、住宿等各大功能区域；教室内闭路电视、电脑、投影仪等设备齐全；语音室、演播中心、计算机房、电化教室、多媒体教室、电子阅览室等设施齐备；室内篮球馆、室外篮球场、排球场、足球场以及艺术体操房等场馆资源丰富，可供师生开展各类教育教学活动。

            五爱高级中学现有27个教学班，学生近1100人。在校教职工138人，其中高级教师25人，一级教师43人，区学科带头人1人。学校具有一支德才兼备、敬业爱岗、作风扎实、乐于奉献的教师队伍，教学氛围和谐、融洽。在教育改革形势下，学校领导与教师积极学习二期课改理念，转变教育观念、学习并掌握因特网技术、多媒体课件技术以及IT激励教学技术等现代化教学手段，努力探究研究性学习的教学模式，师生共同努力以创建“学习化校园”。
            """;

    public static void main(String[] args) throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8)
                + "\n" + Files.readString(Path.of("src/main/resources/application-cloud.yml"), StandardCharsets.UTF_8);
        try (var connection = DriverManager.getConnection(resolved(yaml, "url"), resolved(yaml, "username"), resolved(yaml, "password"))) {
            connection.setAutoCommit(false);
            int id;
            try {
                try (var check = connection.prepareStatement("SELECT id FROM score_query_tool_school WHERE name = ?")) {
                    check.setString(1, NAME);
                    try (var result = check.executeQuery()) {
                        if (result.next()) throw new IllegalStateException("School already exists at ID " + result.getInt(1));
                    }
                }
                try (var insert = connection.prepareStatement(
                        "INSERT INTO score_query_tool_school (name, short_name, district, school_type, address, description) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, NAME);
                    insert.setString(2, SHORT_NAME);
                    insert.setString(3, DISTRICT);
                    insert.setString(4, SCHOOL_TYPE);
                    insert.setString(5, ADDRESS);
                    insert.setString(6, DESCRIPTION.strip());
                    if (insert.executeUpdate() != 1) throw new IllegalStateException("Unexpected insert count");
                    try (var keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) throw new IllegalStateException("No generated ID");
                        id = keys.getInt(1);
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
            try (var verify = connection.prepareStatement(
                    "SELECT name, short_name, district, school_type, address, description FROM score_query_tool_school WHERE id = ?")) {
                verify.setInt(1, id);
                try (var result = verify.executeQuery()) {
                    if (!result.next() || !NAME.equals(result.getString(1)) || !SHORT_NAME.equals(result.getString(2))
                            || !DISTRICT.equals(result.getString(3)) || !SCHOOL_TYPE.equals(result.getString(4))
                            || !ADDRESS.equals(result.getString(5)) || !DESCRIPTION.strip().equals(result.getString(6))) {
                        throw new IllegalStateException("Exact verification failed");
                    }
                }
            }
            System.out.println("Insert and exact verification OK: ID=" + id);
        }
    }

    private static String resolved(String yaml, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + ":\\s*([^\\r\\n#]+)").matcher(yaml);
        if (!matcher.find()) throw new IllegalStateException("Missing DB config: " + key);
        String raw = matcher.group(1).trim();
        Matcher placeholder = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}").matcher(raw);
        if (!placeholder.matches()) return raw;
        String environment = System.getenv(placeholder.group(1));
        if (environment != null && !environment.isBlank()) return environment;
        if (placeholder.group(2) != null) return placeholder.group(2);
        throw new IllegalStateException("Missing environment variable: " + placeholder.group(1));
    }
}
