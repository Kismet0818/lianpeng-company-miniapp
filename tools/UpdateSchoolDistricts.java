import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateSchoolDistricts {
    public static void main(String[] args) throws Exception {
        boolean update = args.length == 1 && "--update".equals(args[0]);
        Map<Integer, String> districts = districts();
        if (districts.size() != 92) throw new IllegalStateException("District mapping must contain 92 rows");

        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8)
                + "\n" + Files.readString(Path.of("src/main/resources/application-cloud.yml"), StandardCharsets.UTF_8);
        try (Connection connection = DriverManager.getConnection(
                resolved(yaml, "url"), resolved(yaml, "username"), resolved(yaml, "password"))) {
            int found = 0;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, district FROM score_query_tool_school WHERE id BETWEEN 75 AND 166 ORDER BY id");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    found++;
                    int id = result.getInt("id");
                    String current = result.getString("district");
                    if (!districts.containsKey(id)) throw new IllegalStateException("Missing mapping for ID " + id);
                    if (current != null && !current.isBlank() && !current.equals(districts.get(id))) {
                        throw new IllegalStateException("Existing district differs at ID " + id + ": " + current);
                    }
                }
            }
            if (found != 92) throw new IllegalStateException("Expected 92 database rows, found " + found);
            System.out.println("Preflight OK: 92 rows found and no conflicting district values.");
            if (!update) {
                System.out.println("Dry run only. Pass --update to update.");
                return;
            }

            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE score_query_tool_school SET district = ? WHERE id = ?")) {
                for (Map.Entry<Integer, String> entry : districts.entrySet()) {
                    statement.setString(1, entry.getValue());
                    statement.setInt(2, entry.getKey());
                    statement.addBatch();
                }
                int[] results = statement.executeBatch();
                if (results.length != 92) throw new IllegalStateException("Unexpected update count");
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

            int matched = 0;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT district FROM score_query_tool_school WHERE id = ?")) {
                for (Map.Entry<Integer, String> entry : districts.entrySet()) {
                    statement.setInt(1, entry.getKey());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || !entry.getValue().equals(result.getString(1))) {
                            throw new IllegalStateException("Verification failed at ID " + entry.getKey());
                        }
                        matched++;
                    }
                }
            }
            System.out.println("Update and exact verification OK: rows=" + matched);
        }
    }

    private static Map<Integer, String> districts() {
        Map<Integer, String> map = new LinkedHashMap<>();
        add(map, "浦东新区", 75,79,80,86,98,104,106,107,109,112,127,131,140,141,146,155,156,159);
        add(map, "杨浦区", 76,81,87,91,97,110,142,144,147);
        add(map, "金山区", 77,100,133,154,158);
        add(map, "嘉定区", 78,88,89,90,134);
        add(map, "徐汇区", 82,85,102,115,122);
        add(map, "静安区", 83,101,111,114,126,130,139,145,164);
        add(map, "长宁区", 84,99,157,160);
        add(map, "闵行区", 92,105,120,125,152,163);
        add(map, "宝山区", 93,121,123,129,136);
        add(map, "崇明区", 94,95,96,103);
        add(map, "普陀区", 108,113,124,149,166);
        add(map, "黄浦区", 116,117,118,143);
        add(map, "虹口区", 119,132,137,148,165);
        add(map, "青浦区", 128,138);
        add(map, "松江区", 135,151,153,161);
        add(map, "奉贤区", 150,162);
        return map;
    }

    private static void add(Map<Integer, String> map, String district, int... ids) {
        for (int id : ids) {
            if (map.put(id, district) != null) throw new IllegalStateException("Duplicate ID " + id);
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
