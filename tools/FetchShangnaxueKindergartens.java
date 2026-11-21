import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 抓取上哪学公开的上海幼儿园列表和详情；不复用 HAR 中的 openid、Cookie 或登录码。 */
public final class FetchShangnaxueKindergartens {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private static final String BASE = "https://api.shangnaxue.net/cis/api/ma";
    private static final Set<String> SHANGHAI_DISTRICTS = Set.of(
            "黄浦区", "徐汇区", "长宁区", "静安区", "普陀区", "虹口区", "杨浦区", "浦东新区",
            "闵行区", "宝山区", "嘉定区", "金山区", "松江区", "青浦区", "奉贤区", "崇明区");

    public static void main(String[] args) throws Exception {
        boolean listOnly = java.util.List.of(args).contains("--list-only");
        String date = LocalDate.now().toString().replace("-", "");
        Path directory = Path.of("tmp", "shangnaxue-kindergartens-" + date);
        Path output = Path.of("tmp", "shangnaxue-kindergartens-" + date + ".json");
        Files.createDirectories(directory);

        Map<String, JsonNode> all = new LinkedHashMap<>();
        int total = Integer.MAX_VALUE;
        int pageSize = 100;
        for (int page = 1; all.size() < total; page++) {
            String listUrl = BASE + "/school/list?size=" + pageSize + "&current=" + page + "&name=&cityId=101"
                    + "&longitude=&latitude=&isSysAppointment=0&isSysMidway=0&typeId=&areaId="
                    + "&segmentGradeId=2&natureId=";
            JsonNode listRoot = get(listUrl);
            requireSuccess(listRoot, "幼儿园列表第" + page + "页");
            JSON.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("list-" + page + ".json").toFile(), listRoot);
            JsonNode data = listRoot.path("data");
            total = data.path("total").asInt();
            int before = all.size();
            for (JsonNode row : data.path("records")) {
                if ("幼儿园".equals(row.path("segmentGradeName").asText())) {
                    all.put(row.path("schoolId").asText(), row);
                }
            }
            System.out.printf("LIST page=%d rawUnique=%d total=%d%n", page, all.size(), total);
            if (all.size() == before) throw new IllegalStateException("列表分页未前进: page=" + page);
            Thread.sleep(400);
        }
        if (all.size() != total) {
            throw new IllegalStateException("列表未一次取全或存在重复: total=" + total + ", unique=" + all.size());
        }
        Map<String, JsonNode> unique = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : all.entrySet()) {
            JsonNode row = entry.getValue();
            if ("上海市".equals(row.path("cityName").asText())
                    && SHANGHAI_DISTRICTS.contains(row.path("areaName").asText())) {
                unique.put(entry.getKey(), row);
            }
        }
        System.out.printf("LIST rawTotal=%d shanghai=%d%n", total, unique.size());

        ArrayNode schools = JSON.createArrayNode();
        String openid = System.getenv("SHANGNAXUE_OPENID");
        int completed = 0;
        for (Map.Entry<String, JsonNode> entry : unique.entrySet()) {
            String schoolId = entry.getKey();
            Path detailFile = directory.resolve("detail-" + schoolId + ".json");
            JsonNode detailRoot;
            if (listOnly) {
                ObjectNode fallback = JSON.createObjectNode();
                fallback.put("code", 0);
                fallback.set("data", entry.getValue());
                detailRoot = fallback;
            } else if (Files.exists(detailFile)) {
                detailRoot = JSON.readTree(detailFile.toFile());
            } else {
                String detailUrl = BASE + "/school/detail?channel=&schoolId="
                        + URLEncoder.encode(schoolId, StandardCharsets.UTF_8)
                        + (openid == null || openid.isBlank() ? "" : "&openid=" + URLEncoder.encode(openid, StandardCharsets.UTF_8))
                        + "&latitude=&longitude=&promotionEventInfoId=";
                detailRoot = get(detailUrl);
                requireSuccess(detailRoot, "学校详情 " + schoolId);
                JSON.writerWithDefaultPrettyPrinter().writeValue(detailFile.toFile(), detailRoot);
                Thread.sleep(400);
            }
            ObjectNode item = JSON.createObjectNode();
            item.set("list", entry.getValue());
            item.set("detail", detailRoot.path("data"));
            schools.add(item);
            completed++;
            if (completed % 10 == 0 || completed == unique.size()) {
                System.out.printf("DETAIL %d/%d%n", completed, unique.size());
            }
        }

        ObjectNode result = JSON.createObjectNode();
        result.put("source", "shangnaxue-public-api");
        result.put("city", "上海");
        result.put("segmentGradeId", 2);
        result.put("rawTotal", total);
        result.put("detailsAvailable", !listOnly);
        result.put("count", schools.size());
        result.set("schools", schools);
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), result);
        System.out.println("OUTPUT=" + output.toAbsolutePath());
    }

    private static JsonNode get(String url) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("Accept", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (compatible; lpsx-kindergarten-import/1.0)")
                        .GET().build();
                HttpResponse<String> response = HTTP.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode());
                return JSON.readTree(response.body());
            } catch (Exception exception) {
                last = exception;
                Thread.sleep(1000L * attempt);
            }
        }
        throw last;
    }

    private static void requireSuccess(JsonNode root, String label) {
        if (root.path("code").asInt(-1) != 0 || root.path("data").isMissingNode()) {
            throw new IllegalStateException(label + "响应失败: code=" + root.path("code").asText());
        }
    }
}
