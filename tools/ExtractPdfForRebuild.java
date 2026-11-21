import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

public class ExtractPdfForRebuild {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ExtractPdfForRebuild <input.pdf> <output-dir>");
        }
        File input = new File(args[0]);
        Path outputDir = Path.of(args[1]);
        Files.createDirectories(outputDir);
        try (PDDocument document = Loader.loadPDF(input)) {
            PDFTextStripper stripper = new PDFTextStripper();
            Files.writeString(outputDir.resolve("text.txt"), stripper.getText(document), StandardCharsets.UTF_8);
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 180, ImageType.RGB);
                ImageIO.write(image, "png", outputDir.resolve(String.format("page-%02d.png", page + 1)).toFile());
            }
            System.out.println("pages=" + document.getNumberOfPages());
        }
    }
}
