# Multi-stage Dockerfile for LPG-EHL Application
# Stage 1: Build with Maven
FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy pom files first for better layer caching
COPY pom.xml .
COPY lpg-ehl-core/pom.xml lpg-ehl-core/
COPY lpg-ehl-emulator/pom.xml lpg-ehl-emulator/
COPY lpg-ehl-api/pom.xml lpg-ehl-api/

# Download dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY lpg-ehl-core/src lpg-ehl-core/src
COPY lpg-ehl-emulator/src lpg-ehl-emulator/src
COPY lpg-ehl-api/src lpg-ehl-api/src

# Build application (skip tests in Docker build, run separately)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime with JRE only
FROM eclipse-temurin:21-jre-alpine

# Install required packages
RUN apk add --no-cache \
    curl \
    bash \
    && rm -rf /var/cache/apk/*

# Create application user (security best practice)
RUN addgroup -g 1000 lpg && \
    adduser -D -u 1000 -G lpg lpg

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/lpg-ehl-api/target/lpg-ehl-api-*.jar /app/app.jar

# Create directories for logs and config
RUN mkdir -p /app/logs /app/config && \
    chown -R lpg:lpg /app

# Switch to non-root user
USER lpg

# Expose ports
EXPOSE 8080 5005

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Labels for metadata
LABEL maintainer="Norges Gass"
LABEL application="lpg-ehl"
LABEL description="LPG Dispenser EHL Protocol Controller"
