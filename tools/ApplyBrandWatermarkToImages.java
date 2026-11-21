import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;

/** Adds a scaled translucent brand logo to curated article images, writing non-destructive copies. */
public class ApplyBrandWatermarkToImages {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: <brand.png> <input-dir> <output-dir>");
        BufferedImage logo = ImageIO.read(Path.of(args[0]).toFile());
        Path in = Path.of(args[1]), out = Path.of(args[2]); Files.createDirectories(out);
        List<String> names = List.of("doc_0701.png","doc_0702.png","doc_0703.png","doc_0704.png",
            "doc_0801.png","doc_0802.png","doc_0803.png","doc_0901.png","doc_0902.png",
            "doc_1101.png","doc_1102.png","doc_1103.png","doc_1104.png","2023上海中考科目和分值设置_无水印重制版.png");
        for (String name : names) {
            BufferedImage src = ImageIO.read(in.resolve(name).toFile());
            if (src == null) throw new IllegalStateException("Unreadable: " + name);
            BufferedImage dst = new BufferedImage(src.getWidth(),src.getHeight(),BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=dst.createGraphics(); g.drawImage(src,0,0,null);
            int lw=Math.max(90,(int)(src.getWidth()*0.22));
            int lh=Math.max(1,lw*logo.getHeight()/logo.getWidth());
            int margin=Math.max(8,(int)(Math.min(src.getWidth(),src.getHeight())*0.025));
            int pad=Math.max(5,lw/25), x=src.getWidth()-margin-lw, y=src.getHeight()-margin-lh;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.72f));
            g.setColor(Color.WHITE); g.fillRoundRect(x-pad,y-pad,lw+pad*2,lh+pad*2,pad*2,pad*2);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.68f));
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(logo,x,y,lw,lh,null); g.dispose();
            Path target=out.resolve(name); ImageIO.write(dst,"png",target.toFile());
            System.out.println(name+"\t"+src.getWidth()+"x"+src.getHeight()+"\t"+Files.size(target));
        }
    }
}
