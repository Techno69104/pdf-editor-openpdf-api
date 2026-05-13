package com.pdfeditor.api.model;

import com.pdfeditor.api.dto.TextBlockDTO;
import java.util.List;

public class UploadResponse {
    private String fileId;
    private int totalPages;
    private List<PageData> pages;
    private String html;
    private String fileName;

    public static class PageData {
        private int pageNum;
        private float width;
        private float height;
        private List<TextBlockDTO> textBlocks;
        private String imageUrl;

        public int getPageNum() { return pageNum; }
        public void setPageNum(int pageNum) { this.pageNum = pageNum; }

        public float getWidth() { return width; }
        public void setWidth(float width) { this.width = width; }

        public float getHeight() { return height; }
        public void setHeight(float height) { this.height = height; }

        public List<TextBlockDTO> getTextBlocks() { return textBlocks; }
        public void setTextBlocks(List<TextBlockDTO> textBlocks) { this.textBlocks = textBlocks; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public List<PageData> getPages() { return pages; }
    public void setPages(List<PageData> pages) { this.pages = pages; }

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
