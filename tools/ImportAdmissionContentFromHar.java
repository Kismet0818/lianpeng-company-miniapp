import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Imports the article groups captured in 详细文章 等等.har into local lpsx only. */
public class ImportAdmissionContentFromHar {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern DOC = Pattern.compile("doc_(\\d{2})(\\d{2})\\.(?:png|jpe?g)", Pattern.CASE_INSENSITIVE);
    private static final String LOCAL_ROOT = "志愿填报小知识";

    private record Article(String no, String title, Integer year, int sort) {}
    private record Page(int page, String url, String fileName) {}

    private static final List<Article> KNOWLEDGE = List.of(
            new Article("01", "高中阶段可以报考哪些类型的学校", null, 1),
            new Article("07", "上海中考政策性照顾加分和同分优待项目说明", null, 7),
            new Article("08", "上海中考招生查询录取去向官方网站公众号汇总", null, 8),
            new Article("09", "上海市高中自主招生志愿、名额分配综合评价录取和统一招生录取志愿信息样表", null, 9),
            new Article("10", "2024年上海闵行、宝山、浦东、松江、青浦5区共新增15所高中介绍", 2024, 10),
            new Article("11", "图说上海市中招志愿类别和招生录取批次", null, 11),
            new Article("12", "什么是招生代码，在填报志愿时怎么用", null, 12),
            new Article("13", "上海高中自主招生录取志愿有哪几个类别", null, 13),
            new Article("14", "上海中考名额分配综合评价录取志愿介绍", null, 14),
            new Article("15", "上海中考统一招生录取志愿介绍", null, 15),
            new Article("16", "上海中考末位同分时怎么处理", null, 16),
            new Article("17", "2024年上海市特色普通高中自主招生情况介绍", 2024, 17),
            new Article("18", "2024年体育“一条龙”市级优秀体育学生招收学校和项目汇总", 2024, 18),
            new Article("19", "2024年艺术“一条龙”市级艺术骨干学生招收学校、项目和艺术团汇总", 2024, 19),
            new Article("22", "2025年上海徐汇区、普陀区、虹口区、闵行区、嘉定区、浦东新区、松江区7区共新增11所高中介绍", 2025, 22),
            new Article("23", "2025年体育“一条龙”市级优秀体育学生招收学校和项目汇总", 2025, 23),
            new Article("24", "2025年艺术“一条龙”市级艺术骨干学生招收学校和项目汇总", 2025, 24),
            new Article("25", "上海20所市特色普通高中2025年自主招生情况汇总", 2025, 25),
            new Article("missing-subject-score", "中考有哪些科目，分数如何分配", null, 2)
    );

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: ImportAdmissionContentFromHar <har> <image-root>");
        Path harPath = Path.of(args[0]);
        Path imageRoot = Path.of(args[1]);
        Map<String, List<Page>> pages = readPages(harPath);

        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) throw new IllegalStateException("DB_PASSWORD is required");
        String jdbc = "jdbc:mysql://127.0.0.1:3306/lpsx?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
        try (Connection connection = DriverManager.getConnection(jdbc, "root", password)) {
            connection.setAutoCommit(false);
            String sql = "INSERT INTO admission_content_article " +
                    "(source_platform,source_category,source_article_no,title,article_year,content_format,content_text,source_url,image_urls,local_image_files,image_count,capture_status,capture_note,sort_order) " +
                    "VALUES ('kylx365',?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE title=VALUES(title),article_year=VALUES(article_year),content_format=VALUES(content_format),content_text=VALUES(content_text),source_url=VALUES(source_url),image_urls=VALUES(image_urls),local_image_files=VALUES(local_image_files),image_count=VALUES(image_count),capture_status=VALUES(capture_status),capture_note=VALUES(capture_note),sort_order=VALUES(sort_order)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (Article article : KNOWLEDGE) {
                    List<Page> articlePages = pages.getOrDefault(article.no(), List.of());
                    bind(ps, "规划须知/志愿填报小知识", article, null, null, articlePages, imageRoot,
                            articlePages.isEmpty() ? "title_only" : "complete",
                            articlePages.isEmpty() ? "标题来自用户截图，HAR 未捕获该条正文图片" : "正文以 HAR 捕获的按序图片页保存");
                    ps.addBatch();
                }
                Article detailed = new Article("detail-2026-new-17-high-schools",
                        "2026新开17所高中介绍（黄浦、徐汇、静安、普陀、闵行等）", 2026, 1);
                bind(ps, "详细文章", detailed, null,
                        "https://www.shobserver.cn/sgh/detail?id=1743421", List.of(), imageRoot,
                        "title_only", "标题来自用户截图；HAR 未捕获正文，公开参考来源为上海教育考试院信息的媒体转载，未将 HAR 中游戏广告图片作为正文");
                ps.addBatch();
                ps.executeBatch();
            }
            connection.commit();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT source_category,capture_status,COUNT(*) cnt,SUM(image_count) images FROM admission_content_article WHERE source_platform='kylx365' GROUP BY source_category,capture_status ORDER BY source_category,capture_status");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) System.out.printf("%s\t%s\t%d\t%d%n", rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4));
            }
        }
    }

    private static Map<String, List<Page>> readPages(Path harPath) throws Exception {
        String text = Files.readString(harPath, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);
        JsonNode entries = JSON.readTree(text).path("log").path("entries");
        Map<String, List<Page>> result = new LinkedHashMap<>();
        for (JsonNode entry : entries) {
            String url = entry.path("request").path("url").asText();
            Matcher matcher = DOC.matcher(url);
            if (!matcher.find() || !url.contains("/web/_ScoreLine/")) continue;
            String articleNo = matcher.group(1);
            int pageNo = Integer.parseInt(matcher.group(2));
            result.computeIfAbsent(articleNo, ignored -> new ArrayList<>())
                    .add(new Page(pageNo, url, matcher.group()));
        }
        result.values().forEach(list -> list.sort(Comparator.comparingInt(Page::page)));
        return result;
    }

    private static void bind(PreparedStatement ps, String category, Article article, String contentText,
                             String sourceUrl, List<Page> pages, Path imageRoot,
                             String status, String note) throws Exception {
        List<String> urls = pages.stream().map(Page::url).toList();
        List<String> files = pages.stream().map(page -> LOCAL_ROOT + "/" + page.fileName()).toList();
        ps.setString(1, category);
        ps.setString(2, article.no());
        ps.setString(3, article.title());
        if (article.year() == null) ps.setNull(4, java.sql.Types.SMALLINT); else ps.setInt(4, article.year());
        ps.setString(5, pages.isEmpty() ? "metadata_only" : "image_pages");
        ps.setString(6, contentText);
        ps.setString(7, sourceUrl);
        ps.setString(8, JSON.writeValueAsString(urls));
        ps.setString(9, JSON.writeValueAsString(files));
        ps.setInt(10, pages.size());
        ps.setString(11, status);
        ps.setString(12, note + "；本地检查根目录：" + imageRoot.toAbsolutePath());
        ps.setInt(13, article.sort());
    }
}
