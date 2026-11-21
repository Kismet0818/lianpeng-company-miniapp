import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UploadNumberedSchoolLogos {
    private record Item(int id, String school, Path file, String objectName, String url) {}

    public static void main(String[] args) throws Exception {
        Path source = Path.of("C:/Users/Admin/Desktop/校徽3");
        Path config = Path.of("src/main/resources/application.yml");
        Path report = Path.of("school-logo-mapping-75-166.csv");
        boolean upload = args.length == 1 && "--upload".equals(args[0]);
        boolean verify = args.length == 1 && "--verify".equals(args[0]);

        String yaml = Files.readString(config, StandardCharsets.UTF_8);
        String endpoint = value(yaml, "endpoint");
        String accessKeyId = resolvedValue(yaml, "access-key-id");
        String accessKeySecret = resolvedValue(yaml, "access-key-secret");
        String bucket = value(yaml, "bucket-name");

        List<Path> files;
        try (var stream = Files.list(source)) {
            files = stream.filter(Files::isRegularFile)
                    .filter(UploadNumberedSchoolLogos::isImage)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            int id = 75 + i;
            String name = file.getFileName().toString();
            String ext = name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            String school = name.substring(0, name.lastIndexOf('.'));
            String object = "score-query/school-logo/" + id + ext;
            String url = "https://" + bucket + "." + endpoint + "/" + object;
            items.add(new Item(id, school, file, object, url));
        }

        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            List<String> conflicts = items.stream()
                    .filter(item -> client.doesObjectExist(bucket, item.objectName()))
                    .map(Item::objectName).toList();
            if (verify) {
                if (conflicts.size() != items.size()) {
                    throw new IllegalStateException("Verification failed: expected " + items.size()
                            + " objects, found " + conflicts.size());
                }
                System.out.println("Verification OK: all " + conflicts.size() + " OSS objects exist.");
                return;
            }
            if (!conflicts.isEmpty()) {
                throw new IllegalStateException("OSS objects already exist; upload aborted: " + conflicts);
            }
            System.out.printf("Preflight OK: %d files, IDs %d-%d, no conflicts.%n",
                    items.size(), items.get(0).id(), items.get(items.size() - 1).id());
            if (!upload) {
                System.out.println("Dry run only. Pass --upload to upload.");
                return;
            }
            int done = 0;
            for (Item item : items) {
                client.putObject(bucket, item.objectName(), item.file().toFile());
                done++;
                System.out.printf("[%d/%d] %d %s%n", done, items.size(), item.id(), item.school());
            }
            writeReport(report, items);
            System.out.println("REPORT=" + report.toAbsolutePath());
        } finally {
            client.shutdown();
        }
    }

    private static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".webp") || name.endsWith(".bmp");
    }

    private static String value(String yaml, String key) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + ":\\s*([^\\r\\n#]+)").matcher(yaml);
        if (!matcher.find()) throw new IllegalStateException("Missing config: " + key);
        return matcher.group(1).trim();
    }

    private static String resolvedValue(String yaml, String key) {
        String raw = value(yaml, key);
        Matcher matcher = Pattern.compile("\\$\\{([^:}]+):([^}]+)}").matcher(raw);
        if (!matcher.matches()) return raw;
        String fromEnv = System.getenv(matcher.group(1));
        return fromEnv == null || fromEnv.isBlank() ? matcher.group(2) : fromEnv;
    }

    private static void writeReport(Path report, List<Item> items) throws IOException {
        StringBuilder csv = new StringBuilder("编号,学校,文件名,OSS地址\r\n");
        for (Item item : items) {
            csv.append(item.id()).append(',')
                    .append(quote(item.school())).append(',')
                    .append(quote(item.file().getFileName().toString())).append(',')
                    .append(item.url()).append("\r\n");
        }
        Files.writeString(report, "\uFEFF" + csv, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
