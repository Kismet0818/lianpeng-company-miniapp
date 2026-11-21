import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Adds the standard non-destructive brand mark to explicitly selected images. */
public class ApplyBrandWatermarkToSelectedImages {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) throw new IllegalArgumentException("Usage: <brand.png> <input-dir> <output-dir> <file>...");
        BufferedImage logo = ImageIO.read(Path.of(args[0]).toFile());
        Path in = Path.of(args[1]), out = Path.of(args[2]);
        Files.createDirectories(out);
        for (int i = 3; i < args.length; i++) {
            String name = args[i];
            BufferedImage src = ImageIO.read(in.resolve(name).toFile());
            if (src == null) throw new IllegalStateException("Unreadable: " + name);
            BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.drawImage(src, 0, 0, null);
            int lw = Math.max(90, (int) (src.getWidth() * 0.22));
            int lh = Math.max(1, lw * logo.getHeight() / logo.getWidth());
            int margin = Math.max(8, (int) (Math.min(src.getWidth(), src.getHeight()) * 0.025));
            int pad = Math.max(5, lw / 25), x = src.getWidth() - margin - lw, y = src.getHeight() - margin - lh;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
            g.setColor(Color.WHITE);
            g.fillRoundRect(x - pad, y - pad, lw + pad * 2, lh + pad * 2, pad * 2, pad * 2);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.68f));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(logo, x, y, lw, lh, null);
            g.dispose();
            Path target = out.resolve(name);
            ImageIO.write(dst, "png", target.toFile());
            System.out.println(name + "\t" + src.getWidth() + "x" + src.getHeight() + "\t" + Files.size(target));
        }
    }
}
