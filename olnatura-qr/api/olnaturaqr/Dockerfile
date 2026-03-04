# Multi-stage: build with Gradle, run with JRE
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle wrapper + build config (cache layer when only src changes)
COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Source and build (skip test for faster image build)
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN adduser -D -s /bin/sh appuser
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 3001
ENTRYPOINT ["java", "-jar", "app.jar"]
