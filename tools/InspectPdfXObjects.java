import java.io.File;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class InspectPdfXObjects {
  static void inspect(PDResources resources, String indent) throws Exception {
    if (resources == null) return;
    for (COSName name : resources.getXObjectNames()) {
      PDXObject x = resources.getXObject(name);
      if (x instanceof PDImageXObject img) {
        System.out.printf("%s%s IMAGE %dx%d%n", indent, name.getName(), img.getWidth(), img.getHeight());
      } else if (x instanceof PDFormXObject form) {
        System.out.printf("%s%s FORM bbox=%s len=%d%n", indent, name.getName(), form.getBBox(), form.getCOSObject().getInt(COSName.LENGTH));
        inspect(form.getResources(), indent + "  ");
      } else {
        System.out.printf("%s%s %s%n", indent, name.getName(), x.getClass().getSimpleName());
      }
    }
  }
  public static void main(String[] args) throws Exception {
    try (PDDocument doc=Loader.loadPDF(new File(args[0]))) {
      int p=0; for(PDPage page:doc.getPages()){System.out.println("PAGE "+(++p)); inspect(page.getResources(), "  ");}
    }
  }
}
