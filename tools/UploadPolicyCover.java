import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 上传单张政策封面到自有 OSS；默认拒绝覆盖已有对象。 */
public final class UploadPolicyCover {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: UploadPolicyCover <policySourceId> <imageFile> <objectName>");
        Path file = Path.of(args[1]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("图片不存在: " + file);
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        String endpoint=value(yaml,"endpoint"), bucket=value(yaml,"bucket-name");
        String accessId=resolvedValue(yaml,"access-key-id"), secret=resolvedValue(yaml,"access-key-secret");
        String objectName=args[2];
        OSS client=new OSSClientBuilder().build(endpoint,accessId,secret);
        try {
            if(client.doesObjectExist(bucket,objectName)) throw new IllegalStateException("OSS 对象已存在，拒绝覆盖: "+objectName);
            client.putObject(bucket,objectName,file.toFile());
            if(!client.doesObjectExist(bucket,objectName)) throw new IllegalStateException("上传后校验失败");
            System.out.println("POLICY_SOURCE_ID="+args[0]);
            System.out.println("OSS_URL=https://"+bucket+"."+endpoint+"/"+objectName);
        } finally { client.shutdown(); }
    }
    private static String value(String yaml,String key){Matcher m=Pattern.compile("(?m)^\\s*"+Pattern.quote(key)+":\\s*([^\\r\\n#]+)").matcher(yaml);if(!m.find())throw new IllegalStateException("缺少配置: "+key);return m.group(1).trim();}
    private static String resolvedValue(String yaml,String key){String raw=value(yaml,key);Matcher m=Pattern.compile("\\$\\{([^:}]+):([^}]+)}").matcher(raw);if(!m.matches())return raw;String env=System.getenv(m.group(1));return env==null||env.isBlank()?m.group(2):env;}
}
