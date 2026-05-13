# PDF Editor OpenPDF API

A Java Spring Boot API using **OpenPDF** (iText open-source fork) and **PDFBox** for exact PDF layout preservation and editing.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   User Upload   │────▶│  Java Spring Boot │────▶│  OpenPDF +      │
│   PDF File      │     │  API (this repo)  │     │  PDFBox         │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  1. Render pages │
                       │     as images    │
                       │  2. Extract text │
                       │     with exact   │
                       │     positions    │
                       │  3. Generate     │
                       │     editable HTML│
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  Frontend edits  │
                       │  text in browser │
                       └──────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │  Download as     │
                       │  PDF or HTML     │
                       └──────────────────┘
```

## Tech Stack

| Component | Library | Purpose |
|-----------|---------|---------|
| PDF Reading | OpenPDF (iText fork) | Extract text, fonts, positions |
| PDF Rendering | Apache PDFBox | Convert pages to high-res images |
| PDF Writing | OpenPDF | Generate edited PDF output |
| Web Framework | Spring Boot 3.2 | REST API |
| Build Tool | Maven | Dependency management |

## Why OpenPDF?

**OpenPDF** is a fork of iText 4.x, maintained by the LibrePDF community:
- ✅ **Open source** (LGPL/MPL dual license)
- ✅ **Mature & stable** - based on proven iText 4 codebase
- ✅ **Active maintenance** - regular updates
- ✅ **PDF generation** - create/modify PDFs programmatically
- ✅ **Font handling** - embed custom fonts
- ✅ **No watermarks** - unlike some commercial alternatives

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/upload` | Upload PDF, return editable HTML |
| GET | `/preview/{filename}` | Get page preview image |
| POST | `/download` | Download edited HTML |
| POST | `/download-pdf` | Generate edited PDF (OpenPDF) |
| GET | `/health` | Health check |

## Deploy to Render

### Step 1: Create Repository

1. Create new GitHub repository
2. Push all files from this project
3. Make sure `system.properties` is in root (tells Render to use Java 17)

### Step 2: Create Render Service

1. Go to [render.com](https://render.com)
2. Click **New +** → **Web Service**
3. Connect your GitHub repository
4. Configure:
   - **Name**: `pdf-editor-openpdf-api`
   - **Runtime**: `Java`
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -jar target/pdf-editor-openpdf-api-1.0.0.jar`
   - **Plan**: Free

5. Add Environment Variables:
   ```
   RENDER_EXTERNAL_URL = https://pdf-editor-openpdf-api.onrender.com
   ```

6. Click **Create Web Service**

### Step 3: Verify Deployment

Wait for build to complete, then test:
```bash
curl https://pdf-editor-openpdf-api.onrender.com/health
```

Expected response:
```json
{"status": "ok", "service": "PDF Editor OpenPDF API v1.0"}
```

## Local Development

### Requirements
- Java 17+
- Maven 3.8+

### Run locally
```bash
# Clone repository
git clone <your-repo-url>
cd pdf-editor-openpdf-api

# Build
./mvnw clean package -DskipTests

# Run
java -jar target/pdf-editor-openpdf-api-1.0.0.jar

# Or use Spring Boot Maven plugin
./mvnw spring-boot:run
```

API will be available at `http://localhost:8080`

### Test upload
```bash
curl -X POST -F "file=@your-document.pdf" http://localhost:8080/upload
```

## Frontend Integration

Update your `pdf-editor.php` to use the new API:

```javascript
const API_BASE = "https://pdf-editor-openpdf-api.onrender.com";
```

The response format is the same as before:
```json
{
  "file_id": "uuid",
  "total_pages": 1,
  "pages": [...],
  "html": "<div class="pdf-page">...</div>",
  "file_name": "document.pdf"
}
```

## Project Structure

```
pdf-editor-openpdf-api/
├── src/main/java/com/pdfeditor/api/
│   ├── PdfEditorApiApplication.java
│   ├── config/
│   │   └── CorsConfig.java
│   ├── controller/
│   │   └── PdfController.java
│   ├── service/
│   │   └── PdfService.java
│   ├── model/
│   │   ├── UploadResponse.java
│   │   └── DownloadRequest.java
│   └── dto/
│       └── TextBlockDTO.java
├── src/main/resources/
│   └── application.properties
├── uploads/              # PDF storage
├── outputs/              # Edited PDF output
├── pom.xml               # Maven config
├── system.properties     # Java version for Render
├── render.yaml           # Render deployment config
└── README.md             # This file
```

## License

OpenPDF is licensed under LGPL/MPL. This project follows the same licensing.

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request
