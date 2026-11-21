import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FindSchool {
    public static void main(String[] args) throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8)
                + "\n" + Files.readString(Path.of("src/main/resources/application-cloud.yml"), StandardCharsets.UTF_8);
        try (var connection = DriverManager.getConnection(resolved(yaml, "url"), resolved(yaml, "username"), resolved(yaml, "password"));
             var statement = connection.prepareStatement(
                     "SELECT id, HEX(short_name), HEX(boarding_info) FROM score_query_tool_school WHERE id=163")) {
            try (var result = statement.executeQuery()) {
                int count = 0;
                while (result.next()) {
                    count++;
                    System.out.println(result.getInt(1) + "\t" + result.getString(2) + "\t" + result.getString(3));
                }
                System.out.println("MATCHES=" + count);
            }
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
