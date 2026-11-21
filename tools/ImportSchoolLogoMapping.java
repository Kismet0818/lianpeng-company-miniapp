import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImportSchoolLogoMapping {
    private record Row(int id, String name, String logo) {}

    public static void main(String[] args) throws Exception {
        boolean insert = args.length == 1 && "--insert".equals(args[0]);
        boolean verify = args.length == 1 && "--verify".equals(args[0]);
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8)
                + "\n" + Files.readString(Path.of("src/main/resources/application-cloud.yml"), StandardCharsets.UTF_8);
        String url = resolved(yaml, "url");
        String username = resolved(yaml, "username");
        String password = resolved(yaml, "password");
        List<Row> rows = readCsv(Path.of("school-logo-mapping-75-166.csv"));

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            List<Integer> existing = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM score_query_tool_school WHERE id BETWEEN 75 AND 166 ORDER BY id");
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) existing.add(result.getInt(1));
            }
            if (verify) {
                int matched = 0;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT name, logo FROM score_query_tool_school WHERE id = ?")) {
                    for (Row row : rows) {
                        statement.setInt(1, row.id());
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next() || !row.name().equals(result.getString(1))
                                    || !row.logo().equals(result.getString(2)) || result.next()) {
                                throw new IllegalStateException("Mismatch at ID " + row.id());
                            }
                            matched++;
                        }
                    }
                }
                System.out.println("Exact verification OK: id/name/logo matched rows=" + matched);
                return;
            }
            if (!existing.isEmpty()) {
                throw new IllegalStateException("IDs 75-166 already contain records; insertion aborted: " + existing);
            }
            System.out.println("Preflight OK: IDs 75-166 are empty; rows=" + rows.size());
            if (!insert) {
                System.out.println("Dry run only. Pass --insert to insert.");
                return;
            }

            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO score_query_tool_school (id, name, logo) VALUES (?, ?, ?)")) {
                for (Row row : rows) {
                    statement.setInt(1, row.id());
                    statement.setString(2, row.name());
                    statement.setString(3, row.logo());
                    statement.addBatch();
                }
                int[] results = statement.executeBatch();
                if (results.length != rows.size()) throw new IllegalStateException("Unexpected batch result count");
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

            int count;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM score_query_tool_school WHERE id BETWEEN 75 AND 166");
                 ResultSet result = statement.executeQuery()) {
                result.next();
                count = result.getInt(1);
            }
            if (count != rows.size()) throw new IllegalStateException("Verification failed; found rows=" + count);
            System.out.println("Insert and verification OK: rows=" + count);
        }
    }

    private static List<Row> readCsv(Path path) throws Exception {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = Pattern.compile("^(\\d+),\"((?:\"\"|[^\"])*)\",\"(?:\"\"|[^\"])*\",(https://.+)$").matcher(line);
            if (!matcher.matches()) throw new IllegalStateException("Invalid CSV row " + (i + 1));
            rows.add(new Row(Integer.parseInt(matcher.group(1)), matcher.group(2).replace("\"\"", "\""), matcher.group(3)));
        }
        if (rows.size() != 92 || rows.get(0).id() != 75 || rows.get(rows.size() - 1).id() != 166) {
            throw new IllegalStateException("Unexpected mapping range/count");
        }
        return rows;
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
