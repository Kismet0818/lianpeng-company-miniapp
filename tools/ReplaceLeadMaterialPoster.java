import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Explicitly replaces the fixed lead-material startup poster and verifies remote bytes. */
public final class ReplaceLeadMaterialPoster {
    private static final String OBJECT = "lead-materials/lingziliao.png";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: ReplaceLeadMaterialPoster <png-file>");
        Path file = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("File not found: " + file);
        byte[] local = Files.readAllBytes(file);
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        String endpoint = value(yaml, "endpoint");
        String bucket = value(yaml, "bucket-name");
        OSS client = new OSSClientBuilder().build(endpoint, resolved(yaml, "access-key-id"), resolved(yaml, "access-key-secret"));
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/png");
            metadata.setCacheControl("no-cache, no-store, must-revalidate");
            metadata.setContentLength(local.length);
            client.putObject(new PutObjectRequest(bucket, OBJECT, file.toFile(), metadata));
            byte[] remote;
            try (InputStream in = client.getObject(bucket, OBJECT).getObjectContent()) {
                remote = in.readAllBytes();
            }
            if (!MessageDigest.isEqual(sha256(local), sha256(remote))) {
                throw new IllegalStateException("OSS verification failed: remote bytes differ");
            }
            ObjectMetadata actual = client.getObjectMetadata(bucket, OBJECT);
            System.out.println("OSS_URL=https://" + bucket + "." + endpoint + "/" + OBJECT);
            System.out.println("BYTES=" + actual.getContentLength());
            System.out.println("SHA256=" + HexFormat.of().formatHex(sha256(remote)));
            System.out.println("CACHE_CONTROL=" + actual.getCacheControl());
        } finally {
            client.shutdown();
        }
    }

    private static byte[] sha256(byte[] bytes) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    private static String value(String yaml, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + ":\\s*([^\\r\\n#]+)").matcher(yaml);
        if (!matcher.find()) throw new IllegalStateException("Missing config: " + key);
        return matcher.group(1).trim();
    }

    private static String resolved(String yaml, String key) {
        String raw = value(yaml, key);
        Matcher matcher = Pattern.compile("\\$\\{([^:}]+):([^}]+)}").matcher(raw);
        if (!matcher.matches()) return raw;
        String env = System.getenv(matcher.group(1));
        return env == null || env.isBlank() ? matcher.group(2) : env;
    }
}
