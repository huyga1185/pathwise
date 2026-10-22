# syntax=docker/dockerfile:1
# Multi-stage image (ARCHITECTURE.md §12.1). Build context: be/

FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace
# Resolve dependencies in their own layer so source-only changes reuse it.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
# Tests run in CI, not during the image build.
RUN ./mvnw -B -q package -DskipTests \
    && cp target/pathwise-*.jar app.jar

FROM maven:3.9-eclipse-temurin-26 AS extract
WORKDIR /workspace
COPY --from=build /workspace/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# Ubuntu-based JRE: OR-Tools needs glibc, so no Alpine.
FROM eclipse-temurin:21-jre AS runtime
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=extract --chown=app:app /workspace/extracted/dependencies/ ./
COPY --from=extract --chown=app:app /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=app:app /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=app:app /workspace/extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
