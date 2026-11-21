import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Removes known top-right source badges and applies one centered low-opacity Lianpeng mark. */
public class BuildFinalAdmissionArticleImages {
    private record Item(String input, String output, double clearFromX, double clearToY) {}
    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Usage: <brand.png> <new-image-dir> <original-dir> <output-dir>");
        BufferedImage logo = ImageIO.read(Path.of(args[0]).toFile());
        Path fresh = Path.of(args[1]), original = Path.of(args[2]), out = Path.of(args[3]);
        Files.createDirectories(out);
        Item[] freshItems = {
            new Item("上海中考政策性照顾加分及同分优待（新doc_0703）.png", "07-required-chart.png", .70, .18),
            new Item("上海中考招生查询录取去向（新doc_0802＋doc_0803）.png", "08-required-chart.png", .55, .10),
            new Item("新1103.png", "11-required-flow.png", .80, .17),
            new Item("？？？.png", "11-required-schedule.png", .80, .17)
        };
        for (Item item : freshItems) process(fresh.resolve(item.input), out.resolve(item.output), logo, item.clearFromX, item.clearToY);
        process(original.resolve("doc_0902.png"), out.resolve("09-sample-form.png"), logo, -1, -1);
        process(original.resolve("2023上海中考科目和分值设置_无水印重制版.png"), out.resolve("subject-score.png"), logo, -1, -1);
    }
    private static void process(Path input, Path output, BufferedImage logo, double clearFromX, double clearToY) throws Exception {
        BufferedImage src = ImageIO.read(input.toFile());
        if (src == null) throw new IllegalStateException("Unreadable: " + input);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, null);
        if (clearFromX >= 0) {
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(Color.WHITE);
            g.fillRect((int)(src.getWidth() * clearFromX), 0, src.getWidth(), (int)(src.getHeight() * clearToY));
        }
        int lw = Math.max(120, (int)(src.getWidth() * .28));
        int lh = lw * logo.getHeight() / logo.getWidth();
        int x = (src.getWidth() - lw) / 2, y = (src.getHeight() - lh) / 2;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .14f));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(logo, x, y, lw, lh, null);
        g.dispose();
        ImageIO.write(dst, "png", output.toFile());
        System.out.println(output.getFileName() + "\t" + src.getWidth() + "x" + src.getHeight() + "\t" + Files.size(output));
    }
}
