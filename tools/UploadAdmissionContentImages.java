import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import java.nio.file.*;
import java.util.*;

/** Uploads the curated admission article images to an isolated OSS prefix. Does not write databases. */
public class UploadAdmissionContentImages {
    private record Item(String articleNo, String fileName) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: UploadAdmissionContentImages <image-root>");
        String endpoint = "oss-cn-shanghai.aliyuncs.com";
        String bucket = "lianpeng";
        String accessKey = requiredEnv("OSS_ACCESS_KEY_ID");
        String secret = requiredEnv("OSS_SECRET");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        List<Item> items = List.of(
            new Item("07", "doc_0701.png"), new Item("07", "doc_0702.png"),
            new Item("07", "doc_0703.png"), new Item("07", "doc_0704.png"),
            new Item("08", "doc_0801.png"), new Item("08", "doc_0802.png"), new Item("08", "doc_0803.png"),
            new Item("09", "doc_0901.png"), new Item("09", "doc_0902.png"),
            new Item("11", "doc_1101.png"), new Item("11", "doc_1102.png"),
            new Item("11", "doc_1103.png"), new Item("11", "doc_1104.png"),
            new Item("missing-subject-score", "2023上海中考科目和分值设置_无水印重制版.png")
        );
        OSS oss = new OSSClientBuilder().build(endpoint, accessKey, secret);
        try {
            for (Item item : items) {
                Path file = root.resolve(item.fileName()).normalize();
                if (!file.startsWith(root) || !Files.isRegularFile(file) || Files.size(file) == 0)
                    throw new IllegalStateException("Missing/empty image: " + file);
                String page = item.fileName().startsWith("doc_")
                    ? item.fileName().substring(item.fileName().length() - 8, item.fileName().length() - 4)
                    : "01";
                String object = "admission-content/" + item.articleNo() + "/" + page + ".png";
                if (oss.doesObjectExist(bucket, object))
                    throw new IllegalStateException("OSS object already exists; refusing overwrite: " + object);
            }
            for (Item item : items) {
                Path file = root.resolve(item.fileName()).normalize();
                String page = item.fileName().startsWith("doc_")
                    ? item.fileName().substring(item.fileName().length() - 8, item.fileName().length() - 4)
                    : "01";
                String object = "admission-content/" + item.articleNo() + "/" + page + ".png";
                oss.putObject(bucket, object, file.toFile());
                long remote = oss.getObjectMetadata(bucket, object).getContentLength();
                if (remote != Files.size(file)) throw new IllegalStateException("Length mismatch: " + object);
                System.out.println(item.articleNo() + "\t" + "https://" + bucket + "." + endpoint + "/" + object + "\t" + remote);
            }
        } finally {
            oss.shutdown();
        }
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing environment variable: " + name);
        return value;
    }
}
