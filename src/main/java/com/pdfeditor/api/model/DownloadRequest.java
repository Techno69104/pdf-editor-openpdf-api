package com.pdfeditor.api.model;

import java.util.Map;

public class DownloadRequest {
    private String fileId;
    private String htmlContent;
    private Map<String, String> textChanges;

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }

    public Map<String, String> getTextChanges() { return textChanges; }
    public void setTextChanges(Map<String, String> textChanges) { this.textChanges = textChanges; }
}
