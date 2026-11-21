import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts article image responses from a Chrome HAR and downloads bodies omitted by HAR. */
public class ExtractArticleImagesFromHar {
    private static final Pattern DOC_NAME = Pattern.compile("(doc_(\\d{2})(\\d{2})\\.(?:png|jpe?g))", Pattern.CASE_INSENSITIVE);
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ExtractArticleImagesFromHar <input.har> <output-dir>");
        }
        Path harPath = Path.of(args[0]);
        Path outputRoot = Path.of(args[1]);
        Path knowledgeDir = outputRoot.resolve("志愿填报小知识");
        Path detailedDir = outputRoot.resolve("详细文章");
        Files.createDirectories(knowledgeDir);
        Files.createDirectories(detailedDir);

        String harJson = Files.readString(harPath, StandardCharsets.UTF_8);
        if (!harJson.isEmpty() && harJson.charAt(0) == '\uFEFF') harJson = harJson.substring(1);
        JsonNode root = JSON.readTree(harJson);
        JsonNode entries = root.path("log").path("entries");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        Set<String> seen = new HashSet<>();
        List<String> manifest = new ArrayList<>();
        manifest.add("category\tarticle_no\tpage_no\tfile_name\tmime_type\tbytes\tsha256\tsource_url\tsource");
        int extracted = 0;

        for (JsonNode entry : entries) {
            String url = entry.path("request").path("url").asText();
            if (!seen.add(url)) continue;
            String mime = entry.path("response").path("content").path("mimeType").asText();
            Matcher docMatcher = DOC_NAME.matcher(url);
            boolean isKnowledge = docMatcher.find() && url.contains("miaguo-1253256735.file.myqcloud.com/web/_ScoreLine/");
            // The wximg responses in this HAR were inspected and are game advertisements,
            // not images belonging to the requested detailed education article.
            boolean isDetailed = false;
            if (!isKnowledge && !isDetailed) continue;

            String category = isKnowledge ? "志愿填报小知识" : "详细文章";
            String articleNo = isKnowledge ? docMatcher.group(2) : "detail";
            String pageNo = isKnowledge ? docMatcher.group(3) : String.format(Locale.ROOT, "%02d", extracted + 1);
            String extension = extensionFor(url, mime);
            String fileName = isKnowledge ? docMatcher.group(1) : "detail_" + pageNo + extension;
            Path destination = (isKnowledge ? knowledgeDir : detailedDir).resolve(fileName);

            JsonNode content = entry.path("response").path("content");
            String body = content.path("text").asText("");
            byte[] bytes;
            String source;
            if (!body.isEmpty() && "base64".equalsIgnoreCase(content.path("encoding").asText())) {
                bytes = Base64.getDecoder().decode(body);
                source = "har";
            } else {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(45))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET().build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Download failed: HTTP " + response.statusCode() + " " + url);
                }
                bytes = response.body();
                source = "download";
            }
            Files.write(destination, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            manifest.add(String.join("\t", category, articleNo, pageNo, fileName, mime,
                    String.valueOf(bytes.length), hex(MessageDigest.getInstance("SHA-256").digest(bytes)), url, source));
            extracted++;
        }

        Files.write(outputRoot.resolve("图片清单.tsv"), manifest, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("EXTRACTED=" + extracted);
        System.out.println("OUTPUT=" + outputRoot.toAbsolutePath());
    }

    private static String extensionFor(String url, String mime) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".jpeg")) return ".jpeg";
        if (lower.contains(".jpg")) return ".jpg";
        if (mime.contains("png")) return ".png";
        return ".jpg";
    }

    private static String hex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte b : value) result.append(String.format(Locale.ROOT, "%02x", b));
        return result.toString();
    }
}
