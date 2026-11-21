package com.example.lpsx.service;

import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class PdfPreviewPageService {
    private static final Logger log = LoggerFactory.getLogger(PdfPreviewPageService.class);
    private static final String PROMPT_HEADING_IMAGE = "pdf/claim-full-material-heading.png.b64";
    private static final String PROMPT_POSTER_IMAGE = "pdf/claim-full-material-poster.jpg.b64";

    private byte[] promptHeadingImage;
    private byte[] promptPosterImage;

    @PostConstruct
    void loadPromptImage() throws IOException {
        promptHeadingImage = loadBase64Resource(PROMPT_HEADING_IMAGE);
        promptPosterImage = loadBase64Resource(PROMPT_POSTER_IMAGE);
    }

    private byte[] loadBase64Resource(String path) throws IOException {
        try (var input = new ClassPathResource(path).getInputStream()) {
            String encoded = new String(input.readAllBytes(), StandardCharsets.US_ASCII).trim();
            return Base64.getDecoder().decode(encoded);
        }
    }

    /**
     * 保留数据库中的原始预览页，在下载响应末尾追加一张领取完整版的提示页。
     * 数据库文件不被修改，避免重复执行后不断增加提示页。
     */
    public byte[] appendClaimPage(byte[] sourcePdf) {
        return buildPreview(sourcePdf, false);
    }

    /** 指南 OSS 可能保存完整版：预览响应只保留第一页，再追加领取提示页。 */
    public byte[] firstPageWithClaimPage(byte[] sourcePdf) {
        return buildPreview(sourcePdf, true);
    }

    private byte[] buildPreview(byte[] sourcePdf, boolean firstPageOnly) {
        try (PDDocument document = Loader.loadPDF(sourcePdf);
             ByteArrayOutputStream output = new ByteArrayOutputStream(
                     sourcePdf.length + promptHeadingImage.length + promptPosterImage.length)) {
            if (document.getNumberOfPages() == 0) return sourcePdf;
            if (firstPageOnly) {
                while (document.getNumberOfPages() > 1) document.removePage(document.getNumberOfPages() - 1);
            }
            // 提示页固定使用 A4 竖版。即使原文第一页是横版，宣传图也不会被拉伸或裁切。
            PDPage promptPage = new PDPage(PDRectangle.A4);
            document.addPage(promptPage);

            PDImageXObject heading = PDImageXObject.createFromByteArray(
                    document, promptHeadingImage, "claim-full-material-heading");
            PDImageXObject poster = PDImageXObject.createFromByteArray(
                    document, promptPosterImage, "claim-full-material-poster");
            float pageWidth = promptPage.getMediaBox().getWidth();
            float pageHeight = promptPage.getMediaBox().getHeight();
            float horizontalMargin = 28f;
            float contentWidth = pageWidth - horizontalMargin * 2;

            float headingWidth = contentWidth;
            float headingHeight = headingWidth * heading.getHeight() / heading.getWidth();
            float headingX = horizontalMargin;
            float headingY = pageHeight - 26f - headingHeight;

            float posterMaxHeight = headingY - 24f - 32f;
            float posterScale = Math.min(contentWidth / poster.getWidth(), posterMaxHeight / poster.getHeight());
            float posterWidth = poster.getWidth() * posterScale;
            float posterHeight = poster.getHeight() * posterScale;
            float posterX = (pageWidth - posterWidth) / 2;
            float posterY = 32f + (posterMaxHeight - posterHeight) / 2;
            try (PDPageContentStream stream = new PDPageContentStream(document, promptPage)) {
                stream.setNonStrokingColor(248f / 255f, 248f / 255f, 251f / 255f);
                stream.addRect(0, 0, pageWidth, pageHeight);
                stream.fill();
                stream.drawImage(heading, headingX, headingY, headingWidth, headingHeight);
                stream.drawImage(poster, posterX, posterY, posterWidth, posterHeight);
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException error) {
            // 提示页生成异常时仍允许用户查看原始预览页，不能让下载接口整体不可用。
            log.error("追加 PDF 完整版领取提示页失败", error);
            return sourcePdf;
        }
    }
}
