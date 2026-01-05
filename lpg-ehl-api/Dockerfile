# Multi-stage Dockerfile for LPG-EHL API (Spring Boot)

# Stage 1: Build application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and parent pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy all module pom.xml files (required for multi-module resolution)
COPY lpg-ehl-core/pom.xml lpg-ehl-core/
COPY lpg-ehl-emulator/pom.xml lpg-ehl-emulator/
COPY lpg-ehl-api/pom.xml lpg-ehl-api/

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code for both core and api
COPY lpg-ehl-core/src lpg-ehl-core/src
COPY lpg-ehl-api/src lpg-ehl-api/src

# Build the api application (includes core as dependency)
RUN ./mvnw clean package -DskipTests -B -pl lpg-ehl-api -am

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install required packages
RUN apk add --no-cache \
    curl \
    bash

# Create non-root user
RUN addgroup -g 1001 lpg && \
    adduser -D -u 1001 -G lpg lpg

# Copy JAR from builder
COPY --from=builder /app/lpg-ehl-api/target/*.jar app.jar

# Change ownership
RUN chown -R lpg:lpg /app

# Switch to non-root user
USER lpg

# Expose application port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
