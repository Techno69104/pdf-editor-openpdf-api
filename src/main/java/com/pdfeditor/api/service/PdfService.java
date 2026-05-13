package com.pdfeditor.api.service;

import com.pdfeditor.api.dto.TextBlockDTO;
import com.pdfeditor.api.model.UploadResponse;
import com.pdfeditor.api.model.DownloadRequest;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PdfService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.output-dir:outputs}")
    private String outputDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UploadResponse processPdf(MultipartFile file) throws IOException {
        String fileId = UUID.randomUUID().toString();

        Files.createDirectories(Paths.get(uploadDir));
        Files.createDirectories(Paths.get(outputDir));

        String originalPath = uploadDir + "/" + fileId + "_original.pdf";
        file.transferTo(new File(originalPath));

        List<String> imageUrls = renderPagesToImages(originalPath, fileId);
        List<UploadResponse.PageData> pagesData = extractTextWithPositions(originalPath, imageUrls);
        String html = generateHtml(pagesData);

        UploadResponse response = new UploadResponse();
        response.setFileId(fileId);
        response.setTotalPages(pagesData.size());
        response.setPages(pagesData);
        response.setHtml(html);
        response.setFileName(file.getOriginalFilename());

        return response;
    }

    private List<String> renderPagesToImages(String pdfPath, String fileId) throws IOException {
        List<String> imageUrls = new ArrayList<>();

        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 192);
                String imageName = fileId + "_page_" + i + ".png";
                String imagePath = uploadDir + "/" + imageName;
                ImageIO.write(image, "PNG", new File(imagePath));
                imageUrls.add(baseUrl + "/preview/" + imageName);
            }
        }

        return imageUrls;
    }

    private List<UploadResponse.PageData> extractTextWithPositions(String pdfPath, List<String> imageUrls) throws IOException {
        List<UploadResponse.PageData> pagesData = new ArrayList<>();

        try {
            PdfReader reader = new PdfReader(pdfPath);

            for (int pageNum = 1; pageNum <= reader.getNumberOfPages(); pageNum++) {
                UploadResponse.PageData pageData = new UploadResponse.PageData();
                pageData.setPageNum(pageNum);
                pageData.setImageUrl(imageUrls.get(pageNum - 1));

                Rectangle pageSize = reader.getPageSizeWithRotation(pageNum);
                pageData.setWidth(pageSize.getWidth());
                pageData.setHeight(pageSize.getHeight());

                List<TextBlockDTO> textBlocks = extractTextBlocks(reader, pageNum);
                pageData.setTextBlocks(textBlocks);

                pagesData.add(pageData);
            }

            reader.close();
        } catch (Exception e) {
            throw new IOException("Error extracting text: " + e.getMessage(), e);
        }

        return pagesData;
    }

    private List<TextBlockDTO> extractTextBlocks(PdfReader reader, int pageNum) {
        List<TextBlockDTO> blocks = new ArrayList<>();

        try {
            byte[] content = reader.getPageContent(pageNum);
            String contentStr = new String(content);

            TextBlockDTO block = new TextBlockDTO();
            block.setBlockIndex(0);
            block.setText("Text extraction with OpenPDF requires custom content stream parsing. Consider using PDFBox for extraction and OpenPDF for generation.");
            block.setX(50);
            block.setY(50);
            block.setWidth(400);
            block.setHeight(20);
            block.setFont("Arial");
            block.setSize(12);
            block.setColor("#111827");
            block.setBold(false);
            block.setItalic(false);
            block.setPageIndex(pageNum - 1);
            blocks.add(block);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return blocks;
    }

    private String generateHtml(List<UploadResponse.PageData> pages) {
        StringBuilder html = new StringBuilder();

        for (UploadResponse.PageData page : pages) {
            float pw = page.getWidth();
            float ph = page.getHeight();

            html.append("<div class="pdf-page" data-page-index="").append(page.getPageNum() - 1).append("" ");
            html.append("style="position:relative;width:").append(pw).append("px;height:").append(ph).append("px;background:white;">");

            html.append("<img src="").append(page.getImageUrl()).append("" ");
            html.append("style="position:absolute;top:0;left:0;width:100%;height:100%;z-index:1;pointer-events:none;user-select:none;" draggable="false">");

            for (TextBlockDTO block : page.getTextBlocks()) {
                String fontFamily = getFontFamily(block.getFont());
                String weight = block.isBold() ? "bold" : "normal";
                String style = block.isItalic() ? "italic" : "normal";
                float lineHeight = block.getSize() * 1.2f;
                String safeText = escapeHtml(block.getText());

                html.append("<div class="text-block" ");
                html.append("data-block-index="").append(block.getBlockIndex()).append("" ");
                html.append("data-page-index="").append(block.getPageIndex()).append("" ");
                html.append("contenteditable="false" ");
                html.append("style="");
                html.append("position:absolute;");
                html.append("left:").append(block.getX()).append("px;");
                html.append("top:").append(block.getY()).append("px;");
                html.append("width:").append(block.getWidth() + 10).append("px;");
                html.append("min-height:").append(block.getHeight()).append("px;");
                html.append("font-family:").append(fontFamily).append(";");
                html.append("font-size:").append(block.getSize()).append("px;");
                html.append("color:").append(block.getColor()).append(";");
                html.append("font-weight:").append(weight).append(";");
                html.append("font-style:").append(style).append(";");
                html.append("line-height:").append(lineHeight).append("px;");
                html.append("white-space:pre-wrap;");
                html.append("word-wrap:break-word;");
                html.append("cursor:text;");
                html.append("outline:none;");
                html.append("border:2px solid transparent;");
                html.append("border-radius:3px;");
                html.append("padding:2px 4px;");
                html.append("margin:-2px -4px;");
                html.append("z-index:10;");
                html.append("background:transparent;");
                html.append("box-sizing:content-box;");
                html.append("" ");
                html.append("data-original-text="").append(safeText).append("">");
                html.append(safeText);
                html.append("</div>");
            }

            html.append("</div>");
        }

        return html.toString();
    }

    public File generateEditedPdf(DownloadRequest request) throws IOException {
        String fileId = request.getFileId();
        String originalPath = uploadDir + "/" + fileId + "_original.pdf";
        String outputPath = outputDir + "/" + fileId + "_edited.pdf";

        try {
            PdfReader reader = new PdfReader(originalPath);
            Document document = new Document(reader.getPageSizeWithRotation(1));
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            PdfContentByte cb = writer.getDirectContent();

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                document.setPageSize(reader.getPageSizeWithRotation(i));
                document.newPage();

                PdfImportedPage page = writer.getImportedPage(reader, i);
                cb.addTemplate(page, 0, 0);
            }

            document.close();
            writer.close();
            reader.close();

        } catch (Exception e) {
            throw new IOException("Error generating PDF: " + e.getMessage(), e);
        }

        return new File(outputPath);
    }

    private String getFontFamily(String fontName) {
        if (fontName == null) return "Arial, sans-serif";
        String lower = fontName.toLowerCase();
        if (lower.contains("times")) return "Times New Roman, Times, serif";
        if (lower.contains("courier")) return "Courier New, Courier, monospace";
        if (lower.contains("georgia")) return "Georgia, serif";
        if (lower.contains("arial") || lower.contains("helv")) return "Arial, Helvetica, sans-serif";
        if (lower.contains("verdana")) return "Verdana, sans-serif";
        if (lower.contains("tahoma")) return "Tahoma, sans-serif";
        if (lower.contains("impact")) return "Impact, sans-serif";
        if (lower.contains("montserrat")) return "Montserrat, sans-serif";
        if (lower.contains("roboto")) return "Roboto, sans-serif";
        if (lower.contains("open sans")) return "Open Sans, sans-serif";
        if (lower.contains("lato")) return "Lato, sans-serif";
        return "Arial, sans-serif";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    public File getPreviewImage(String filename) {
        return new File(uploadDir + "/" + filename);
    }
}
