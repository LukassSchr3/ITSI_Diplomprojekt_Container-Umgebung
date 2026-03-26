# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /app

# Copy Gradle wrapper and dependency descriptors first (layer caching)
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle* ./

RUN chmod +x gradlew && sed -i 's/\r$//' gradlew

RUN ./gradlew dependencies --no-daemon || true

# Copy source and build the fat JAR
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 5050

ENTRYPOINT ["java", "-jar", "app.jar"]
