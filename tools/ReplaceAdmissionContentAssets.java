import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import java.nio.file.*;

/** Replaces only the reviewed canonical article-image objects passed on the command line. */
public class ReplaceAdmissionContentAssets {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length % 2 != 0) throw new IllegalArgumentException("Usage: <file> <object> [<file> <object> ...]");
        OSS oss = new OSSClientBuilder().build("oss-cn-shanghai.aliyuncs.com", env("OSS_ACCESS_KEY_ID"), env("OSS_SECRET"));
        try {
            for (int i = 0; i < args.length; i += 2) {
                Path file = Path.of(args[i]);
                String object = args[i + 1];
                if (!Files.isRegularFile(file) || Files.size(file) == 0) throw new IllegalStateException("Missing/empty: " + file);
                oss.putObject("lianpeng", object, file.toFile());
                long remote = oss.getObjectMetadata("lianpeng", object).getContentLength();
                if (remote != Files.size(file)) throw new IllegalStateException("Length mismatch: " + object);
                System.out.println("OK\t" + object + "\t" + remote);
            }
        } finally { oss.shutdown(); }
    }
    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + name);
        return value;
    }
}
