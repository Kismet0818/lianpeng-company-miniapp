import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateSchoolProfile {
    private static final int ID = 163;
    private static final String EXPECTED_NAME = "华东理工大学附属闵行科技高级中学";
    private static final String LEGACY_NAME = EXPECTED_NAME;
    private static final String SHORT_NAME = "华理科高";
    private static final String DISTRICT = "闵行区";
    private static final String SCHOOL_TYPE = "公办";
    private static final String ADDRESS = "上海市闵行区银康路820弄";
    private static final String DESCRIPTION = """
            华东理工大学附属闵行科技高级中学（以下简称华理科高）创办于1964年，位于以“瞩汇东西、高新引领”为理念的上海市“人工智能创新试验区”内。学校底蕴丰厚、环境优美、设施完备、信息化特色声誉斐然。学校是上海市文明单位、上海市行为规范示范校、上海市特色高中建设项目学校、上海市教育信息化应用标杆培育校、闵行区实验性示范性高中、上海市高中体育专项化第二批试点学校、闵行区阳光体育特色学校、闵行区乒球一条龙布点学校。学校始终坚持立德树人、五育并举，在校训“敦品励学，以品养慧”引领下，以“成就每一个师生生命的精彩”为办学宗旨，以“一所高起点的未来学校，一所高品质的科技高中，一所内涵丰富的特色高中”为发展目标，以课程建设彰显育人特色、以课堂改革提升教学品质、以精彩教师成就学生精彩，建设一所以信息化、科技为主要特色的一流品牌科技高中。

            华理科高是华东理工大学的附属学校。华东理工大学是中国教育部批准的具有招收高水平运动队资格的高校之一，是中国大学生体育协会乒乓球分会主席单位。培养了刘诗雯等一大批世界乒乓名将，在国际大学生乒乓球比赛中战绩显赫。拥有从小学、中学到大学一条龙培养体系的“乒乓球人才库”。华理常年派遣国家高级教练员和国家专业运动员三人进我校进行乒乓球课程和训练执教工作。同时，我校教师四人拥有乒乓球中级教练员或国家一级裁判资质，设有乒乓球专项课程和乒乓球专业训练课程体系，培养了一大批乒乓球特长学生，并在市区各级各类乒乓球比赛屡获佳绩。
            """;

    public static void main(String[] args) throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8)
                + "\n" + Files.readString(Path.of("src/main/resources/application-cloud.yml"), StandardCharsets.UTF_8);
        try (Connection connection = DriverManager.getConnection(
                resolved(yaml, "url"), resolved(yaml, "username"), resolved(yaml, "password"))) {
            connection.setAutoCommit(false);
            try {
                if (!columnExists(connection, "score_query_tool_school", "short_name")) {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("ALTER TABLE score_query_tool_school ADD COLUMN short_name VARCHAR(100) NULL COMMENT '学校名称缩写' AFTER name");
                    }
                }
                String actualName;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT name FROM score_query_tool_school WHERE id = ? FOR UPDATE")) {
                    statement.setInt(1, ID);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalStateException("School ID not found: " + ID);
                        actualName = result.getString(1);
                    }
                }
                if (!EXPECTED_NAME.equals(actualName) && !LEGACY_NAME.equals(actualName)) {
                    throw new IllegalStateException("School name mismatch: " + actualName);
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE score_query_tool_school SET name=?, short_name=?, district=?, school_type=?, address=?, description=? WHERE id=?")) {
                    statement.setString(1, EXPECTED_NAME);
                    statement.setString(2, SHORT_NAME);
                    statement.setString(3, DISTRICT);
                    statement.setString(4, SCHOOL_TYPE);
                    statement.setString(5, ADDRESS);
                    statement.setString(6, DESCRIPTION.strip());
                    statement.setInt(7, ID);
                    if (statement.executeUpdate() != 1) throw new IllegalStateException("Unexpected update count");
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT name, short_name, district, school_type, address, description FROM score_query_tool_school WHERE id=?")) {
                statement.setInt(1, ID);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()
                            || !EXPECTED_NAME.equals(result.getString("name"))
                            || !Objects.equals(SHORT_NAME, result.getString("short_name"))
                            || !DISTRICT.equals(result.getString("district"))
                            || !SCHOOL_TYPE.equals(result.getString("school_type"))
                            || !ADDRESS.equals(result.getString("address"))
                            || !DESCRIPTION.strip().equals(result.getString("description"))) {
                        throw new IllegalStateException("Exact verification failed");
                    }
                }
            }
            System.out.println("Schema update and exact profile verification OK: ID=" + ID);
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet result = metadata.getColumns(connection.getCatalog(), null, table, column)) {
            return result.next();
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
