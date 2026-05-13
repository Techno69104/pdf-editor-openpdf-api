FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy Maven files first for better caching
COPY pom.xml .
COPY src ./src

# Build the application
RUN apk add --no-cache maven &&     mvn clean package -DskipTests

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/pdf-editor-openpdf-api-1.0.0.jar"]
