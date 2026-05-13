# FIX: Build Failed - mvnw Not Found

## Problem
```
bash: line 1: ./mvnw: No such file or directory
```

## Solution
Use system Maven (`mvn`) instead of Maven wrapper (`./mvnw`).

## Updated Commands

| Setting | Old (Broken) | New (Fixed) |
|---------|-------------|-------------|
| **Build Command** | `./mvnw clean package -DskipTests` | `mvn clean package -DskipTests` |
| **Start Command** | `java -jar target/pdf-editor-openpdf-api-1.0.0.jar` | `java -jar target/pdf-editor-openpdf-api-1.0.0.jar` |

## Steps to Fix on Render

1. Go to your Render dashboard
2. Open `pdf-editor-openpdf-api` service
3. Click **Settings** tab
4. Change **Build Command** to:
   ```
   mvn clean package -DskipTests
   ```
5. Click **Save Changes**
6. Click **Manual Deploy** → **Deploy latest commit**

## Alternative: Use Dockerfile

If Maven is not available, use Docker:

1. In Render dashboard, change **Runtime** to `Docker`
2. Build Command: `(leave empty)`
3. Start Command: `(leave empty)`
4. Render will use the `Dockerfile` automatically
