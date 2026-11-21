import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import java.io.File;

public class OssUploadTemp {
  public static void main(String[] args) {
    String endpoint = "https://oss-cn-shanghai.aliyuncs.com";
    String bucket = "lianpeng";
    String key = args[2];
    String file = args[3];
    OSS oss = new OSSClientBuilder().build(endpoint, args[0], args[1]);
    try {
      ObjectMetadata meta = new ObjectMetadata();
      meta.setContentType("image/jpeg");
      oss.putObject(bucket, key, new File(file), meta);
      System.out.println("https://" + bucket + ".oss-cn-shanghai.aliyuncs.com/" + key);
    } finally { oss.shutdown(); }
  }
}
