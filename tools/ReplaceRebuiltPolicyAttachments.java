import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 用已核验的无水印 PDF 覆盖四个既有 OSS 对象，并刷新本地附件记录。 */
public final class ReplaceRebuiltPolicyAttachments {
    record Item(long attachmentId, String localName, String objectKey) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: ReplaceRebuiltPolicyAttachments <source-dir>");
        Path sourceDir = Path.of(args[0]);
        Map<Long, Item> items = new LinkedHashMap<>();
        items.put(6L, new Item(6, "262_16区部分学校市重率_无水印重制版.pdf", "education-policy/19/262_16区部分学校市重率.pdf"));
        items.put(14L, new Item(14, "276_上海16区小升初入学方式_无水印重制版.pdf", "education-policy/15/276_上海16区小升初入学方式.pdf"));
        items.put(4L, new Item(4, "333_2026年到区分数线_无水印重制版.pdf", "education-policy/21/333_2026年到区分数线.pdf"));
        items.put(2L, new Item(2, "337_26各区名额到校分数线(1)_无水印重制版.pdf", "education-policy/22/337_26各区名额到校分数线(1).pdf"));

        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        String dbUrl = value(yaml, "url").replace("192.168.3.5", "127.0.0.1");
        String dbUser = value(yaml, "username");
        String dbPassword = resolved(yaml, "password");
        String endpoint = value(yaml, "endpoint");
        String bucket = value(yaml, "bucket-name");
        String accessKey = resolved(yaml, "access-key-id");
        String secret = resolved(yaml, "access-key-secret");
        String baseUrl = "https://" + bucket + "." + endpoint + "/";

        for (Item item : items.values()) {
            Path file = sourceDir.resolve(item.localName());
            if (!Files.isRegularFile(file) || Files.size(file) < 1000) {
                throw new IllegalStateException("无效本地文件: " + file);
            }
        }

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            for (Item item : items.values()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT file_url FROM education_policy_attachment WHERE id=? AND is_deleted=0")) {
                    statement.setLong(1, item.attachmentId());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalStateException("附件记录不存在: " + item.attachmentId());
                        String expected = baseUrl + item.objectKey();
                        if (!expected.equals(result.getString(1))) {
                            throw new IllegalStateException("数据库 URL 与预期对象不一致: id=" + item.attachmentId());
                        }
                    }
                }
            }
        }

        OSS oss = new OSSClientBuilder().build(endpoint, accessKey, secret);
        try {
            for (Item item : items.values()) {
                Path file = sourceDir.resolve(item.localName());
                oss.putObject(bucket, item.objectKey(), file.toFile());
                long remoteLength = oss.getObjectMetadata(bucket, item.objectKey()).getContentLength();
                if (remoteLength != Files.size(file)) {
                    throw new IllegalStateException("OSS 长度校验失败: " + item.objectKey());
                }
                System.out.println("OSS_OK\t" + item.attachmentId() + "\t" + remoteLength);
            }
        } finally {
            oss.shutdown();
        }

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE education_policy_attachment SET file_url=?, update_time=CURRENT_TIMESTAMP WHERE id=? AND is_deleted=0")) {
                for (Item item : items.values()) {
                    update.setString(1, baseUrl + item.objectKey());
                    update.setLong(2, item.attachmentId());
                    if (update.executeUpdate() != 1) throw new IllegalStateException("数据库更新失败: " + item.attachmentId());
                }
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        }

        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        for (Item item : items.values()) {
            Path file = sourceDir.resolve(item.localName());
            String localHash = sha256(Files.newInputStream(file));
            URI uri = URI.create(baseUrl + item.objectKey() + "?verify=" + System.nanoTime());
            HttpResponse<InputStream> response = http.send(
                    HttpRequest.newBuilder(uri).header("Cache-Control", "no-cache").GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP 验证失败: " + response.statusCode());
            String remoteHash;
            try (InputStream body = response.body()) { remoteHash = sha256(body); }
            if (!localHash.equals(remoteHash)) throw new IllegalStateException("HTTP SHA-256 不一致: " + item.objectKey());
            System.out.println("HTTP_HASH_OK\t" + item.attachmentId() + "\t" + localHash);
        }
        System.out.println("APPLIED_AND_VERIFIED=4");
    }

    static String sha256(InputStream input) throws Exception {
        try (input) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) >= 0;) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest()).toUpperCase();
        }
    }
    static String value(String yaml, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + ":\\s*([^\\r\\n#]+)").matcher(yaml);
        if (!matcher.find()) throw new IllegalStateException("缺少配置: " + key);
        return matcher.group(1).trim().replaceAll("^[\\\"']|[\\\"']$", "");
    }
    static String resolved(String yaml, String key) {
        String raw = value(yaml, key);
        Matcher matcher = Pattern.compile("\\$\\{([^:}]+):([^}]+)}").matcher(raw);
        if (!matcher.matches()) return raw;
        String environment = System.getenv(matcher.group(1));
        return environment == null || environment.isBlank() ? matcher.group(2) : environment;
    }
}
