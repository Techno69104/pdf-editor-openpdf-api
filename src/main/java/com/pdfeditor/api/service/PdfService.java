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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path outputPathDir = Paths.get(outputDir).toAbsolutePath().normalize();

        Files.createDirectories(uploadPath);
        Files.createDirectories(outputPathDir);

        Path originalFile = uploadPath.resolve(fileId + "_original.pdf");
        Files.copy(file.getInputStream(), originalFile, StandardCopyOption.REPLACE_EXISTING);

        String originalPathStr = originalFile.toString();

        List<String> imageUrls = renderPagesToImages(originalPathStr, fileId, uploadPath);
        List<UploadResponse.PageData> pagesData = extractTextWithPositions(originalPathStr, imageUrls);
        String html = generateHtml(pagesData);

        UploadResponse response = new UploadResponse();
        response.setFileId(fileId);
        response.setTotalPages(pagesData.size());
        response.setPages(pagesData);
        response.setHtml(html);
        response.setFileName(file.getOriginalFilename());

        return response;
    }

    private List<String> renderPagesToImages(String pdfPath, String fileId, Path uploadPath) throws IOException {
        List<String> imageUrls = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 192);
                String imageName = fileId + "_page_" + i + ".png";
                Path imagePath = uploadPath.resolve(imageName);
                ImageIO.write(image, "PNG", imagePath.toFile());
                imageUrls.add(baseUrl + "/preview/" + imageName);
            }
        }

        return imageUrls;
    }

    private List<UploadResponse.PageData> extractTextWithPositions(String pdfPath, List<String> imageUrls) throws IOException {
        List<UploadResponse.PageData> pagesData = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(new File(pdfPath))) {
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                UploadResponse.PageData pageData = new UploadResponse.PageData();
                pageData.setPageNum(pageNum + 1);
                pageData.setImageUrl(imageUrls.get(pageNum));

                PDPage page = document.getPage(pageNum);
                PDRectangle mediaBox = page.getMediaBox();
                pageData.setWidth(mediaBox.getWidth());
                pageData.setHeight(mediaBox.getHeight());

                PositionTextStripper stripper = new PositionTextStripper(pageNum, mediaBox.getHeight());
                stripper.setStartPage(pageNum + 1);
                stripper.setEndPage(pageNum + 1);
                stripper.getText(document);

                List<TextBlockDTO> textBlocks = stripper.getBlocks();
                if (textBlocks.isEmpty()) {
                    TextBlockDTO block = new TextBlockDTO();
                    block.setBlockIndex(0);
                    block.setText("No editable text found on this page.");
                    block.setX(50);
                    block.setY(mediaBox.getHeight() / 2);
                    block.setWidth(400);
                    block.setHeight(20);
                    block.setFont("Arial");
                    block.setSize(12);
                    block.setColor("#6b7280");
                    block.setBold(false);
                    block.setItalic(false);
                    block.setPageIndex(pageNum);
                    textBlocks.add(block);
                }

                pageData.setTextBlocks(textBlocks);
                pagesData.add(pageData);
            }
        }

        return pagesData;
    }

    /**
     * Custom PDFBox text stripper that extracts text with exact positions,
     * grouping characters into line-based text blocks.
     */
    private static class PositionTextStripper extends PDFTextStripper {
        private final List<TextBlockDTO> blocks = new ArrayList<>();
        private final List<TextPosition> currentLine = new ArrayList<>();
        private float lastY = -1;
        private int blockIndex = 0;
        private final int pageIndex;
        private final float pageHeight;

        PositionTextStripper(int pageIndex, float pageHeight) throws IOException {
            super();
            this.pageIndex = pageIndex;
            this.pageHeight = pageHeight;
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            for (TextPosition tp : textPositions) {
                float y = tp.getYDirAdj();
                if (lastY != -1 && Math.abs(y - lastY) > 4) {
                    flushLine();
                }
                currentLine.add(tp);
                lastY = y;
            }
        }

        @Override
        public void endPage(PDPage page) throws IOException {
            flushLine();
            super.endPage(page);
        }

        private void flushLine() {
            if (currentLine.isEmpty()) return;

            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = 0;
            float maxY = 0;
            StringBuilder text = new StringBuilder();
            String fontName = "Arial";
            float fontSize = 12;

            for (TextPosition tp : currentLine) {
                float x = tp.getXDirAdj();
                float y = tp.getYDirAdj();
                float w = tp.getWidthDirAdj();
                float h = tp.getHeightDir();

                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
                text.append(tp.getUnicode());

                if (tp.getFont() != null) {
                    String name = tp.getFont().getName();
                    if (name != null && !name.isEmpty()) {
                        fontName = name;
                    }
                }
                fontSize = tp.getFontSizeInPt();
            }

            String lineText = text.toString().trim();
            if (!lineText.isEmpty()) {
                float width = maxX - minX + 10;
                float height = maxY - minY + 4;

                // PDF Y is bottom-origin; HTML is top-origin
                float htmlY = pageHeight - maxY;

                TextBlockDTO block = new TextBlockDTO();
                block.setBlockIndex(blockIndex++);
                block.setText(lineText);
                block.setX(minX);
                block.setY(htmlY);
                block.setWidth(width);
                block.setHeight(height);
                block.setFont(fontName);
                block.setSize(fontSize);
                block.setColor("#111827");
                block.setBold(false);
                block.setItalic(false);
                block.setPageIndex(pageIndex);
                blocks.add(block);
            }

            currentLine.clear();
            lastY = -1;
        }

        List<TextBlockDTO> getBlocks() {
            flushLine();
            return blocks;
        }
    }

    private String generateHtml(List<UploadResponse.PageData> pages) {
        StringBuilder html = new StringBuilder();

        for (UploadResponse.PageData page : pages) {
            float pw = page.getWidth();
            float ph = page.getHeight();

            html.append("<div class=\"pdf-page\" data-page-index=\"")
                .append(page.getPageNum() - 1)
                .append("\" ");
            html.append("style=\"position:relative;width:")
                .append(pw)
                .append("px;height:")
                .append(ph)
                .append("px;background:white;\">");

            html.append("<img src=\"")
                .append(page.getImageUrl())
                .append("\" ");
            html.append("style=\"position:absolute;top:0;left:0;width:100%;height:100%;z-index:1;pointer-events:none;user-select:none;\" draggable=\"false\">");

            for (TextBlockDTO block : page.getTextBlocks()) {
                String fontFamily = getFontFamily(block.getFont());
                String weight = block.isBold() ? "bold" : "normal";
                String style = block.isItalic() ? "italic" : "normal";
                float lineHeight = block.getSize() * 1.2f;
                String safeText = escapeHtml(block.getText());

                html.append("<div class=\"text-block\" ");
                html.append("data-block-index=\"")
                    .append(block.getBlockIndex())
                    .append("\" ");
                html.append("data-page-index=\"")
                    .append(block.getPageIndex())
                    .append("\" ");
                html.append("contenteditable=\"false\" ");
                html.append("style=\"");
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
                html.append("\" ");
                html.append("data-original-text=\"")
                    .append(safeText)
                    .append("\">");
                html.append(safeText);
                html.append("</div>");
            }

            html.append("</div>");
        }

        return html.toString();
    }

    public File generateEditedPdf(DownloadRequest request) throws IOException {
        String fileId = request.getFileId();

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path outputPathDir = Paths.get(outputDir).toAbsolutePath().normalize();
        Files.createDirectories(outputPathDir);

        Path originalFile = uploadPath.resolve(fileId + "_original.pdf");
        Path outputFile = outputPathDir.resolve(fileId + "_edited.pdf");

        try {
            PdfReader reader = new PdfReader(originalFile.toString());
            Document document = new Document(reader.getPageSizeWithRotation(1));
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile.toFile()));
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

        return outputFile.toFile();
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
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        return uploadPath.resolve(filename).toFile();
    }
}
