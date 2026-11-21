import com.example.lpsx.LpsxApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将“2026届中考志愿填报&自招资料”上传到项目OSS并幂等写入本地资源表。
 * 用法：ImportZhongkaoResources <资料根目录>
 */
public class ImportZhongkaoResources {
    private static final String BATCH = "2026-zhongkao-volunteer-self-enrollment";
    private static final Pattern YEAR = Pattern.compile("20(?:2[0-9])");

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: ImportZhongkaoResources <rootDirectory>");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) throw new IllegalArgumentException("Directory not found: " + root);

        List<Path> files;
        try (var paths = Files.walk(root)) {
            files = paths.filter(Files::isRegularFile)
                    .filter(ImportZhongkaoResources::supported)
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .toList();
        }
        if (files.isEmpty()) throw new IllegalArgumentException("No PDF/DOC/DOCX/XLS/XLSX files found");

        SpringApplication app = new SpringApplication(LpsxApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = app.run()) {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            int inserted = 0;
            int reused = 0;
            int order = 1;
            for (Path file : files) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                String sha256 = sha256(file);
                String extension = extension(file);
                Integer existing = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM zhongkao_resource_file WHERE file_sha256 = ?",
                        Integer.class, sha256);
                if (existing != null && existing > 0) {
                    reused++;
                } else {
                    inserted++;
                }

                String fileName = file.getFileName().toString();
                String title = fileName.substring(0, fileName.length() - extension.length() - 1);
                String category = root.getFileName().toString().contains("志愿填报")
                        ? "志愿填报工具包"
                        : (relative.contains("中考真题卷") ? "中考真题卷" : "自招配套资料");
                String subject = subject(relative);
                String type = resourceType(title);
                String sourceYear = sourceYear(title, relative);
                String code = "z2026-" + sha256.substring(0, 16).toLowerCase(Locale.ROOT);
                jdbc.update("""
                        INSERT INTO zhongkao_resource_file
                          (resource_code,title,resource_category,subject_category,resource_type,
                           target_year,source_year,original_file_name,relative_path,file_extension,
                           mime_type,file_size,file_sha256,file_content,oss_url,source_batch,sort_order,status,is_deleted)
                        VALUES (?,?,?,?,?,'2026',?,?,?,?,?,?,?,?,NULL,?,?,1,0)
                        ON DUPLICATE KEY UPDATE
                          title=VALUES(title),resource_category=VALUES(resource_category),
                          subject_category=VALUES(subject_category),resource_type=VALUES(resource_type),
                          source_year=VALUES(source_year),original_file_name=VALUES(original_file_name),
                          relative_path=VALUES(relative_path),file_extension=VALUES(file_extension),
                          mime_type=VALUES(mime_type),file_size=VALUES(file_size),
                          file_content=VALUES(file_content),
                          source_batch=VALUES(source_batch),sort_order=VALUES(sort_order),status=1,is_deleted=0
                        """,
                        code, title, category, subject, type, sourceYear, fileName, relative, extension,
                        mimeType(extension), Files.size(file), sha256, Files.readAllBytes(file), BATCH, order++);
                System.out.println("RESOURCE\t" + code + "\t" + relative + "\tbytes=" + Files.size(file));
            }
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM zhongkao_resource_file WHERE source_batch=? AND is_deleted=0",
                    Integer.class, BATCH);
            Integer uniqueHashes = jdbc.queryForObject(
                    "SELECT COUNT(DISTINCT file_sha256) FROM zhongkao_resource_file WHERE source_batch=? AND is_deleted=0",
                    Integer.class, BATCH);
            System.out.println("SUMMARY\tfiles=" + files.size() + "\tinserted=" + inserted
                    + "\treused=" + reused + "\tdb=" + count + "\tuniqueHashes=" + uniqueHashes);
        }
    }

    private static boolean supported(Path path) {
        String ext = extension(path);
        return List.of("pdf", "doc", "docx", "xls", "xlsx").contains(ext);
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest()).toUpperCase(Locale.ROOT);
    }

    private static String subject(String text) {
        if (text.contains("历史")) return "历史";
        if (text.contains("语文") || text.contains("作文")) return "语文";
        if (text.contains("数学")) return "数学";
        if (text.contains("物理")) return "物理";
        if (text.contains("英语")) return "英语";
        if (text.contains("数理化")) return "综合理科";
        if (text.contains("简历")) return "自招简历";
        if (text.contains("节点") || text.contains("汇总")) return "招考信息";
        return "综合";
    }

    private static String resourceType(String title) {
        if (title.contains("范文")) return "范文";
        if (title.contains("模板") || title.contains("封面")) return "模板";
        if (title.contains("节点") || title.contains("汇总")) return "信息汇总";
        if (title.contains("暂无答案")) return "真题";
        if (title.contains("答案") || title.contains("分析")) return "真题及答案";
        if (title.contains("真题") || title.contains("试卷") || title.contains("数学卷")) return "真题";
        return "其他";
    }

    private static String sourceYear(String title, String relative) {
        Matcher matcher = YEAR.matcher(title);
        if (matcher.find()) return matcher.group();
        if (title.startsWith("26") || relative.contains("2026年上海中考真题卷")) return "2026";
        return null;
    }

    private static String mimeType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }
}
