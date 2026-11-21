import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import java.util.List;

/** Deletes superseded screenshot fragments after canonical replacements are uploaded. */
public class PruneOldAdmissionContentAssets {
    public static void main(String[] args) {
        List<String> objects = List.of(
            "admission-content/07/0701.png", "admission-content/07/0702.png", "admission-content/07/0703.png", "admission-content/07/0704.png",
            "admission-content/08/0801.png", "admission-content/08/0802.png", "admission-content/08/0803.png",
            "admission-content/09/0901.png", "admission-content/09/0902.png",
            "admission-content/11/1101.png", "admission-content/11/1102.png", "admission-content/11/1103.png", "admission-content/11/1104.png"
        );
        OSS oss = new OSSClientBuilder().build("oss-cn-shanghai.aliyuncs.com", env("OSS_ACCESS_KEY_ID"), env("OSS_SECRET"));
        try {
            for (String object : objects) {
                oss.deleteObject("lianpeng", object);
                if (oss.doesObjectExist("lianpeng", object)) throw new IllegalStateException("Delete failed: " + object);
                System.out.println("DELETED\t" + object);
            }
        } finally { oss.shutdown(); }
    }
    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + name);
        return value;
    }
}
