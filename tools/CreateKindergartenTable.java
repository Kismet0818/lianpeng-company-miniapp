import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CreateKindergartenTable {
    public static void main(String[] args) throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        String jdbc = value(yaml, "url").replace("192.168.3.5", "127.0.0.1");
        String sql = Files.readString(Path.of("tools/create_kindergarten.sql"), StandardCharsets.UTF_8).trim();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1);
        try (var connection = DriverManager.getConnection(jdbc, value(yaml, "username"), value(yaml, "password"));
             var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            try (var rows = statement.executeQuery("SELECT TABLE_ROWS,AUTO_INCREMENT FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='kindergarten'")) {
                rows.next();
                System.out.printf("VERIFIED table=kindergarten rows=%d nextId=%d%n", rows.getInt(1), rows.getLong(2));
            }
            try (var rows = statement.executeQuery("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='kindergarten'")) {
                rows.next();
                System.out.printf("columns=%d%n", rows.getInt(1));
            }
        }
    }

    private static String value(String yaml, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + ":\\s*([^\\r\\n#]+)").matcher(yaml);
        if (!matcher.find()) throw new IllegalStateException("缺少数据库配置：" + key);
        String raw = matcher.group(1).trim();
        Matcher placeholder = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}").matcher(raw);
        if (!placeholder.matches()) return raw;
        String environment = System.getenv(placeholder.group(1));
        return environment != null && !environment.isBlank() ? environment : placeholder.group(2);
    }
}
