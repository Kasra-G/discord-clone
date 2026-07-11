# Stage 1: Cache Gradle dependencies
FROM gradle:latest AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY *.gradle.kts gradle.properties /home/gradle/app/
COPY gradle /home/gradle/app/gradle
WORKDIR /home/gradle/app
RUN gradle dependencies --no-daemon
ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.29.0/opentelemetry-javaagent.jar /app/otel-javaagent.jar

# Stage 2: Build Application
FROM gradle:latest AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Build the fat JAR, Gradle also supports shadow
# and boot JAR by default.
RUN gradle buildFatJar --no-daemon

# Stage 3: Create the Runtime Image
FROM amazoncorretto:25 AS runtime
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/server.jar
COPY --from=cache /app/otel-javaagent.jar /app/otel-javaagent.jar

ENV JAVA_TOOL_OPTIONS="-javaagent:/app/otel-javaagent.jar"
ENTRYPOINT ["java","-jar","/app/server.jar"]
