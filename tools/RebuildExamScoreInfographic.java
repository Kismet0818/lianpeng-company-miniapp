import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Deterministically rebuilds the 2023 Shanghai middle-school exam score infographic without source branding. */
public class RebuildExamScoreInfographic {
    private static final int W = 1200;
    private static final int H = 1800;
    private static final Color TEXT = new Color(45, 45, 45);
    private static final Color ORANGE = new Color(255, 104, 72);
    private static final Color PEACH = new Color(255, 218, 205);
    private static final Color GREEN = new Color(92, 142, 46);
    private static final Color PALE_GREEN = new Color(242, 253, 224);
    private static final Color CYAN = new Color(81, 202, 200);
    private static final Color PALE_CYAN = new Color(235, 253, 253);

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: RebuildExamScoreInfographic <output.png>");
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, W, H);

        Font plain = new Font("Microsoft YaHei", Font.PLAIN, 28);
        Font bold = new Font("Microsoft YaHei", Font.BOLD, 34);
        g.setColor(TEXT); g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 30)); g.drawString("2023年", 45, 60);
        g.setFont(new Font("Microsoft YaHei", Font.BOLD, 46)); g.drawString("上海市初中学业水平", 45, 120);
        g.setColor(ORANGE); g.drawString("考试科目和分值设置", 45, 178);
        g.setColor(TEXT); g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 22));
        g.drawString("以下信息仅供参考，请以当年教育主管部门官方发布为准。", 45, 225);
        g.drawLine(40, 245, W - 40, 245);
        g.drawString("投档末位同分时，依次比较语文、数学、英语三科合计成绩、数学、语文、综合测试成绩，高者优先。", 45, 285);

        g.setColor(new Color(255, 249, 247)); g.fillRect(0, 320, W, 450);
        circle(g, 150, 515, 125, PEACH, ORANGE, "语文", "150分", null);
        circle(g, 430, 485, 150, PEACH, ORANGE, "数学", "150分", null);
        circle(g, 740, 515, 125, PEACH, ORANGE, "外语", "150分", new String[]{"笔试满分140分（含听力25分）", "外语听说测试满分10分"});
        circle(g, 1040, 515, 125, PEACH, ORANGE, "综合测试", "150分", new String[]{"物理试题70分", "化学试题50分", "跨学科案例分析15分", "理化实验操作15分"});

        g.setColor(PALE_GREEN); g.fillRect(0, 770, W, 360);
        circle(g, 290, 905, 105, new Color(214, 242, 160), GREEN, "道德与法治", "60分", new String[]{"统一开卷考试30分", "日常考核30分"});
        circle(g, 780, 905, 105, new Color(214, 242, 160), GREEN, "历史", "60分", new String[]{"统一开卷考试30分", "日常考核30分"});

        g.setColor(PALE_CYAN); g.fillRect(0, 1130, W, 410);
        circle(g, 600, 1265, 105, new Color(165, 235, 230), new Color(27, 93, 177), "体育与健身", "30分", new String[]{"统一考试15分", "日常考核15分"});

        g.setColor(TEXT); g.setFont(new Font("Microsoft YaHei", Font.BOLD, 34)); g.drawString("总计", 505, 1690);
        g.setFont(new Font("Microsoft YaHei", Font.BOLD | Font.ITALIC, 60)); g.drawString("750分", 610, 1695);
        g.dispose();
        ImageIO.write(image, "png", output.toFile());
        System.out.println(output.toAbsolutePath());
    }

    private static void circle(Graphics2D g, int cx, int cy, int r, Color fill, Color accent,
                               String label, String score, String[] notes) {
        g.setColor(fill); g.fillOval(cx-r, cy-r, r*2, r*2);
        g.setColor(accent); g.setFont(new Font("Microsoft YaHei", Font.BOLD, label.length() > 5 ? 24 : 30));
        centered(g, label, cx, cy-20);
        g.setFont(new Font("Microsoft YaHei", Font.BOLD | Font.ITALIC, 38)); centered(g, score, cx, cy+28);
        if (notes != null) {
            g.setColor(TEXT); g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 18));
            int y = cy + r + 36;
            for (String note : notes) { centered(g, "• " + note, cx, y); y += 28; }
        }
    }

    private static void centered(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x - fm.stringWidth(text)/2, y);
    }
}
