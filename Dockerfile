# Multi-stage build for Spring Boot application with optimizations

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies in a separate layer (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build (only rebuilds if source changes)
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl (healthcheck) and su-exec (non-root user drop in entrypoint)
RUN apk add --no-cache curl su-exec

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy entrypoint script (runs as root, creates upload dirs, drops to spring)
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Pre-create upload dirs inside the image (used when no volume is mounted)
RUN mkdir -p /app/uploads/po /app/uploads/products && chown -R spring:spring /app

# Expose port 8080
EXPOSE 8080

# Optimized health check with longer start period for Railway
HEALTHCHECK --interval=10s --timeout=5s --start-period=120s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+OptimizeStringConcat -Djava.security.egd=file:/dev/./urandom"

# Run the application via entrypoint (fixes upload dir permissions, then drops to spring user)
ENTRYPOINT ["/docker-entrypoint.sh"]
