# syntax=docker/dockerfile:1.7

# ============================================================================
# Stage 1 — Build
# ============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Cache dependencies first for faster rebuilds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ============================================================================
# Stage 2 — Runtime
# ============================================================================
FROM eclipse-temurin:17-jre-alpine

# Run as non-root user (security best practice)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# wget needed by container healthcheck
RUN apk add --no-cache wget

COPY --from=builder /workspace/target/transaction-service.jar app.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
