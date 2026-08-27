# Stage 1: Build Spring Boot Java API
FROM maven:3.9.6-eclipse-temurin-17 AS backend-builder
WORKDIR /app/backend
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Build Flutter Web App
FROM debian:stable-slim AS frontend-builder
WORKDIR /app
RUN apt-get update && apt-get install -y curl git unzip xz-utils zip && rm -rf /var/lib/apt/lists/*
ENV PATH="/usr/local/flutter/bin:/usr/local/flutter/bin/cache/dart-sdk/bin:${PATH}"
RUN git clone https://github.com/flutter/flutter.git -b stable /usr/local/flutter
COPY . /app
RUN flutter pub get
RUN flutter build web --release

# Stage 3: Unified Production Runtime (Spring Boot API + Flutter Web Nginx Proxy)
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache nginx bash curl sed

WORKDIR /app
COPY --from=backend-builder /app/backend/target/*.jar /app/app.jar
COPY --from=frontend-builder /app/build/web /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80 8085

# Boot Java Spring Boot API on port 8085 and Nginx Reverse Proxy on Railway's $PORT
CMD ["sh", "-c", "SERVER_PORT=8085 java -jar /app/app.jar & sed -i 's/listen 80;/listen '\"${PORT:-80}\"';/g' /etc/nginx/nginx.conf && sed -i 's/listen \\[::\\]:80;/listen \\[::\\]:'\"${PORT:-80}\"';/g' /etc/nginx/nginx.conf && nginx -g 'daemon off;'"]
