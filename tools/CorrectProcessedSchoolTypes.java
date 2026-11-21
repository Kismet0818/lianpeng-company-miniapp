import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CorrectProcessedSchoolTypes {
    private static final Set<Integer> PROCESSED = Set.of(
            82, 83, 84, 85, 99, 101, 102, 111, 114, 115, 116, 117, 118,
            122, 130, 139, 143, 145, 157, 160, 167);
    private static final Set<Integer> PRIVATE = Set.of(122, 145);

    public static void main(String[] args) throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8)
                + "\n" + Files.readString(Path.of("src/main/resources/application-cloud.yml"), StandardCharsets.UTF_8);
        try (var connection = DriverManager.getConnection(resolved(yaml, "url"), resolved(yaml, "username"), resolved(yaml, "password"))) {
            connection.setAutoCommit(false);
            try (var update = connection.prepareStatement(
                    "UPDATE score_query_tool_school SET school_type=? WHERE id=?")) {
                for (int id : PROCESSED) {
                    update.setString(1, PRIVATE.contains(id) ? "民办" : "公办");
                    update.setInt(2, id);
                    update.addBatch();
                }
                if (update.executeBatch().length != PROCESSED.size()) throw new IllegalStateException("Unexpected update count");
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
            int matched = 0;
            try (var query = connection.prepareStatement("SELECT school_type FROM score_query_tool_school WHERE id=?")) {
                for (int id : PROCESSED) {
                    query.setInt(1, id);
                    try (var result = query.executeQuery()) {
                        String expected = PRIVATE.contains(id) ? "民办" : "公办";
                        if (!result.next() || !expected.equals(result.getString(1))) {
                            throw new IllegalStateException("Verification failed at ID " + id);
                        }
                        matched++;
                    }
                }
            }
            System.out.println("School type correction verified: rows=" + matched);
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
