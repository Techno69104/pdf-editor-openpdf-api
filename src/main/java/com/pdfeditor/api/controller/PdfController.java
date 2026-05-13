package com.pdfeditor.api.controller;

import com.pdfeditor.api.model.DownloadRequest;
import com.pdfeditor.api.model.UploadResponse;
import com.pdfeditor.api.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class PdfController {

    @Autowired
    private PdfService pdfService;

    /**
     * Upload PDF and convert to editable HTML
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createError("No file uploaded"));
            }

            if (!file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body(createError("Only PDF files are allowed"));
            }

            UploadResponse response = pdfService.processPdf(file);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(createError("Processing error: " + e.getMessage()));
        }
    }

    /**
     * Get page preview image
     */
    @GetMapping("/preview/{filename}")
    public ResponseEntity<?> getPreview(@PathVariable String filename) {
        File imageFile = pdfService.getPreviewImage(filename);

        if (!imageFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(new FileSystemResource(imageFile));
    }

    /**
     * Download edited HTML
     */
    @PostMapping("/download")
    public ResponseEntity<?> downloadHtml(@RequestBody DownloadRequest request) {
        try {
            if (request.getHtmlContent() == null || request.getHtmlContent().isEmpty()) {
                return ResponseEntity.badRequest().body(createError("No HTML content provided"));
            }

            // Save HTML to file
            String outputPath = "outputs/" + request.getFileId() + "_edited.html";
            java.nio.file.Files.write(
                java.nio.file.Paths.get(outputPath),
                request.getHtmlContent().getBytes()
            );

            File htmlFile = new File(outputPath);
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"edited_document.html\"")
                .body(new FileSystemResource(htmlFile));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(createError("Download error: " + e.getMessage()));
        }
    }

    /**
     * Generate edited PDF (new endpoint)
     */
    @PostMapping("/download-pdf")
    public ResponseEntity<?> downloadPdf(@RequestBody DownloadRequest request) {
        try {
            File pdfFile = pdfService.generateEditedPdf(request);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"edited_document.pdf\"")
                .body(new FileSystemResource(pdfFile));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(createError("PDF generation error: " + e.getMessage()));
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "PDF Editor OpenPDF API v1.0");
        return ResponseEntity.ok(response);
    }

    private Map<String, String> createError(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
