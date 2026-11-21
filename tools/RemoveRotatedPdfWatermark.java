import java.io.File;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.contentstream.operator.Operator;

public class RemoveRotatedPdfWatermark {
    private static boolean nonZero(COSBase value) {
        return value instanceof COSNumber && Math.abs(((COSNumber) value).floatValue()) > 0.0001;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: RemoveRotatedPdfWatermark <input.pdf> <output.pdf>");
        }
        try (PDDocument document = Loader.loadPDF(new File(args[0]))) {
            int removed = 0;
            int removedForms = 0;
            for (PDPage page : document.getPages()) {
                List<Object> tokens = new PDFStreamParser(page).parse();
                List<Object> output = new ArrayList<>(tokens.size());
                List<Object> operands = new ArrayList<>();
                Deque<Boolean> rotationStack = new ArrayDeque<>();
                boolean rotatedCtm = false;
                boolean inText = false;
                boolean rotatedText = false;
                List<Object> textBlock = new ArrayList<>();

                for (Object token : tokens) {
                    if (!(token instanceof Operator op)) {
                        operands.add(token);
                        continue;
                    }
                    String name = op.getName();
                    List<Object> complete = new ArrayList<>(operands);
                    complete.add(op);
                    operands.clear();

                    boolean watermarkForm = !inText && "Do".equals(name) && complete.size() >= 2
                            && complete.get(0) instanceof org.apache.pdfbox.cos.COSName resourceName
                            && resourceName.getName().startsWith("KSPX");
                    if (watermarkForm) {
                        removedForms++;
                        continue;
                    }

                    if (!inText) {
                        if ("q".equals(name)) {
                            rotationStack.push(rotatedCtm);
                        } else if ("Q".equals(name)) {
                            rotatedCtm = rotationStack.isEmpty() ? false : rotationStack.pop();
                        } else if ("cm".equals(name) && complete.size() >= 7) {
                            // Matrix operands: a b c d e f. Non-zero b/c means rotation or skew.
                            boolean thisRotation = nonZero((COSBase) complete.get(1)) || nonZero((COSBase) complete.get(2));
                            rotatedCtm = rotatedCtm || thisRotation;
                        }
                    }

                    if ("BT".equals(name) && !inText) {
                        inText = true;
                        rotatedText = rotatedCtm;
                        textBlock.addAll(complete);
                    } else if (inText) {
                        textBlock.addAll(complete);
                        if ("Tm".equals(name) && complete.size() >= 7) {
                            rotatedText |= nonZero((COSBase) complete.get(1)) || nonZero((COSBase) complete.get(2));
                        }
                        if ("ET".equals(name)) {
                            if (rotatedText) {
                                removed++;
                            } else {
                                output.addAll(textBlock);
                            }
                            textBlock.clear();
                            inText = false;
                            rotatedText = false;
                        }
                    } else {
                        output.addAll(complete);
                    }
                }
                if (inText) {
                    output.addAll(textBlock);
                }
                output.addAll(operands);
                PDStream replacement = new PDStream(document);
                try (OutputStream stream = replacement.createOutputStream()) {
                    new ContentStreamWriter(stream).writeTokens(output);
                }
                page.setContents(replacement);
            }
            document.save(args[1]);
            System.out.println("pages=" + document.getNumberOfPages() + " removedTextBlocks=" + removed
                    + " removedWatermarkForms=" + removedForms);
        }
    }
}
