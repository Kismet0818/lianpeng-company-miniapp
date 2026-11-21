import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** One-off migration: copy primary-school images to OSS and update the cloud DB. */
public class MigratePrimarySchoolImages {
    private static final String DB_URL = "jdbc:mysql://sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com:29693/cloud1-d6g28bq6c04919547?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "lianpeng";
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "Datou870263");
    private static final String OSS_ENDPOINT = "oss-cn-shanghai.aliyuncs.com";
    private static final String OSS_BUCKET = "lianpeng";
    private static final String OSS_KEY_PREFIX = "school-logo/primary/";
    private static final String OSS_ACCESS_KEY = System.getenv("OSS_ACCESS_KEY_ID");
    private static final String OSS_SECRET = System.getenv("OSS_SECRET");

    record Row(int id, String sourceUrl) {}

    public static void main(String[] args) throws Exception {
        List<Row> rows = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement p = c.prepareStatement("SELECT id, background_image_url FROM primary_school WHERE background_image_url IS NOT NULL AND background_image_url <> '' AND background_image_url LIKE 'https://www.anroader.com/%'")) {
            try (ResultSet rs = p.executeQuery()) { while (rs.next()) rows.add(new Row(rs.getInt(1), rs.getString(2))); }
        }

        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        OSS oss = new OSSClientBuilder().build(OSS_ENDPOINT, OSS_ACCESS_KEY, OSS_SECRET);
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            c.setAutoCommit(false);
            try (PreparedStatement update = c.prepareStatement("UPDATE primary_school SET background_image_url=? WHERE id=?")) {
                for (Row row : rows) {
                    HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(URI.create(row.sourceUrl())).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() != 200 || response.body().length == 0) throw new IllegalStateException("download failed: " + row.id() + " HTTP " + response.statusCode());
                    String ext = extension(row.sourceUrl(), response.headers().firstValue("content-type").orElse(""));
                    String object = OSS_KEY_PREFIX + row.id() + ext;
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(response.body().length);
                    metadata.setContentType(response.headers().firstValue("content-type").orElse("image/jpeg"));
                    oss.putObject(OSS_BUCKET, object, new java.io.ByteArrayInputStream(response.body()), metadata);
                    String url = "https://" + OSS_BUCKET + "." + OSS_ENDPOINT + "/" + object;
                    if (!oss.doesObjectExist(OSS_BUCKET, object)) throw new IllegalStateException("OSS verify failed: " + row.id());
                    update.setString(1, url); update.setInt(2, row.id()); update.executeUpdate();
                    System.out.println("MIGRATED\t" + row.id() + "\t" + url);
                }
                c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
        } finally { oss.shutdown(); }
        System.out.println("DONE\t" + rows.size());
    }

    private static String extension(String url, String contentType) {
        String path = URI.create(url).getPath().toLowerCase();
        if (path.endsWith(".png") || contentType.contains("png")) return ".png";
        if (path.endsWith(".webp") || contentType.contains("webp")) return ".webp";
        return ".jpg";
    }
}
