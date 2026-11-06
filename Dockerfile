# Build and test with the same Java major version used at runtime.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean verify

# The runtime image contains only the JRE and the executable application JAR.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/app.jar ./app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
