package com.pdfeditor.api.dto;

public class TextBlockDTO {
    private int blockIndex;
    private String text;
    private float x;
    private float y;
    private float width;
    private float height;
    private String font;
    private float size;
    private String color;
    private boolean bold;
    private boolean italic;
    private int pageIndex;

    public TextBlockDTO() {}

    public int getBlockIndex() { return blockIndex; }
    public void setBlockIndex(int blockIndex) { this.blockIndex = blockIndex; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public String getFont() { return font; }
    public void setFont(String font) { this.font = font; }

    public float getSize() { return size; }
    public void setSize(float size) { this.size = size; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }

    public boolean isItalic() { return italic; }
    public void setItalic(boolean italic) { this.italic = italic; }

    public int getPageIndex() { return pageIndex; }
    public void setPageIndex(int pageIndex) { this.pageIndex = pageIndex; }
}
