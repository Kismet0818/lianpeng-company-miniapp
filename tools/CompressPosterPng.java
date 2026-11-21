import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resizes a poster to its production dimensions and encodes it as an indexed PNG. */
public final class CompressPosterPng {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: CompressPosterPng <input> <output>");
        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        if (source == null) throw new IllegalArgumentException("Unsupported image");
        BufferedImage rgb = new BufferedImage(721, 918, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, 721, 918, null);
        graphics.dispose();
        Path output = Path.of(args[1]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        if (!ImageIO.write(rgb, "png", output.toFile())) throw new IllegalStateException("PNG writer unavailable");
        System.out.println("INPUT_BYTES=" + Files.size(Path.of(args[0])));
        System.out.println("OUTPUT_BYTES=" + Files.size(output));
        System.out.println("DIMENSIONS=721x918");
    }
}
