FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/app.jar
EXPOSE 8081
CMD ["java", "-jar", "-Dserver.port=${PORT:-8081}", "/app/app.jar"]
