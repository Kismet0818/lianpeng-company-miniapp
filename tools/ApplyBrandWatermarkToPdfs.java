import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.nio.file.*;
import java.util.List;

/** Adds a translucent logo at the bottom-right of every PDF page, writing non-destructive copies. */
public class ApplyBrandWatermarkToPdfs {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: <brand.png> <input-dir> <output-dir>");
        Path brand=Path.of(args[0]), in=Path.of(args[1]), out=Path.of(args[2]); Files.createDirectories(out);
        List<String> names=List.of("262_16区部分学校市重率_无水印重制版.pdf","276_上海16区小升初入学方式_无水印重制版.pdf",
            "333_2026年到区分数线_无水印重制版.pdf","337_26各区名额到校分数线(1)_无水印重制版.pdf");
        for(String name:names){
            Path src=in.resolve(name), dst=out.resolve(name.replace("_无水印重制版.pdf","_联朋品牌版.pdf"));
            try(PDDocument doc=Loader.loadPDF(src.toFile())){
                PDImageXObject logo=PDImageXObject.createFromFileByContent(brand.toFile(),doc);
                for(PDPage page:doc.getPages()){
                    PDRectangle old=page.getMediaBox();
                    float pw=old.getWidth(), footer=58f;
                    page.setMediaBox(new PDRectangle(old.getLowerLeftX(),old.getLowerLeftY()-footer,pw,old.getHeight()+footer));
                    page.setCropBox(new PDRectangle(old.getLowerLeftX(),old.getLowerLeftY()-footer,pw,old.getHeight()+footer));
                    float w=pw*0.18f, h=w*logo.getHeight()/logo.getWidth(), margin=8f;
                    try(PDPageContentStream cs=new PDPageContentStream(doc,page,PDPageContentStream.AppendMode.APPEND,true,true)){
                        PDExtendedGraphicsState gs=new PDExtendedGraphicsState(); gs.setNonStrokingAlphaConstant(0.45f);
                        cs.setGraphicsStateParameters(gs); cs.drawImage(logo,old.getLowerLeftX()+pw-margin-w,old.getLowerLeftY()-footer+8f,w,h);
                    }
                }
                doc.save(dst.toFile());
                System.out.println(dst.getFileName()+"\tpages="+doc.getNumberOfPages()+"\tbytes="+Files.size(dst));
            }
        }
    }
}
