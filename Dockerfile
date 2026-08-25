FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-spa tesseract-ocr-data-eng && \
    tesseract --version && tesseract --list-langs && \
    addgroup -S escudo && adduser -S escudo -G escudo
ENV OMP_THREAD_LIMIT=1
WORKDIR /app
COPY --from=backend-build /workspace/backend/target/escudo-backend-0.1.0.jar app.jar
USER escudo
EXPOSE 8080
ENTRYPOINT ["java","-Xms64m","-Xmx256m","-XX:+UseSerialGC","-XX:+UseContainerSupport","-XX:+ExitOnOutOfMemoryError","-jar","/app/app.jar"]

