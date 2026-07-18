FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY *.gradle.kts gradle.properties .

ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.29.0/opentelemetry-javaagent.jar /app/otel-javaagent.jar
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon
COPY src src

RUN --mount=type=cache,target=/root/.gradle ./gradlew buildFatJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=build /app/build/libs/*.jar server.jar
COPY --from=build /app/otel-javaagent.jar otel-javaagent.jar

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/api/health || exit 1

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-javaagent:otel-javaagent.jar"
ENTRYPOINT ["java","-jar","server.jar"]
