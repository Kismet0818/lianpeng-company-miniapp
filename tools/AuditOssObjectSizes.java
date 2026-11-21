import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ListObjectsRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only OSS object size inventory. */
public final class AuditOssObjectSizes {
    public static void main(String[] args) throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);
        String endpoint = value(yaml, "endpoint"), bucket = value(yaml, "bucket-name");
        OSS client = new OSSClientBuilder().build(endpoint, resolved(yaml, "access-key-id"), resolved(yaml, "access-key-secret"));
        List<OSSObjectSummary> objects = new ArrayList<>();
        try {
            String marker = null;
            do {
                ListObjectsRequest request = new ListObjectsRequest(bucket).withMaxKeys(1000);
                request.setMarker(marker);
                ObjectListing listing = client.listObjects(request);
                objects.addAll(listing.getObjectSummaries());
                marker = listing.isTruncated() ? listing.getNextMarker() : null;
            } while (marker != null);
        } finally { client.shutdown(); }

        long total = objects.stream().mapToLong(OSSObjectSummary::getSize).sum();
        Map<String, long[]> byExt = new HashMap<>();
        for (OSSObjectSummary object : objects) {
            String key = object.getKey().toLowerCase();
            int dot = key.lastIndexOf('.');
            String ext = dot < 0 ? "(none)" : key.substring(dot);
            long[] stats = byExt.computeIfAbsent(ext, ignored -> new long[2]);
            stats[0]++; stats[1] += object.getSize();
        }
        System.out.printf("OBJECTS=%d TOTAL_MB=%.2f%n", objects.size(), total / 1048576.0);
        byExt.entrySet().stream().sorted((a,b) -> Long.compare(b.getValue()[1], a.getValue()[1]))
                .forEach(e -> System.out.printf("TYPE\t%s\tcount=%d\tMB=%.2f%n", e.getKey(), e.getValue()[0], e.getValue()[1] / 1048576.0));
        System.out.println("TOP_LARGEST_OBJECTS");
        objects.stream().sorted(Comparator.comparingLong(OSSObjectSummary::getSize).reversed()).limit(100)
                .forEach(o -> System.out.printf("%.2f MB\t%s%n", o.getSize() / 1048576.0, o.getKey()));
    }
    private static String value(String yaml,String key){Matcher m=Pattern.compile("(?m)^\\s*"+Pattern.quote(key)+":\\s*([^\\r\\n#]+)").matcher(yaml);if(!m.find())throw new IllegalStateException("Missing "+key);return m.group(1).trim();}
    private static String resolved(String yaml,String key){String raw=value(yaml,key);Matcher m=Pattern.compile("\\$\\{([^:}]+):([^}]+)}").matcher(raw);if(!m.matches())return raw;String env=System.getenv(m.group(1));return env==null||env.isBlank()?m.group(2):env;}
}
