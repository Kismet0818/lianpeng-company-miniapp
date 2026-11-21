import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import java.nio.file.*;

public class RenderPdfAuditPages {
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]); Files.createDirectories(out);
        for(int i=1;i<args.length;i++){
            Path pdf=Path.of(args[i]);
            try(PDDocument doc=Loader.loadPDF(pdf.toFile())){
                PDFRenderer renderer=new PDFRenderer(doc);
                int[] pages=doc.getNumberOfPages()==1?new int[]{0}:new int[]{0,doc.getNumberOfPages()-1};
                for(int page:pages){Path target=out.resolve("pdf"+i+"_page"+(page+1)+".png"); ImageIO.write(renderer.renderImageWithDPI(page,110),"png",target.toFile());}
            }
        }
    }
}
