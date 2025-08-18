# Multi-stage build: Maven builder + slim JRE runtime

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="" \
    SERVER_PORT=8080 \
    OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://collector:4318/v1/traces
WORKDIR /app
COPY --from=build /app/target/distributed-inventory-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar --server.port=${SERVER_PORT}"]

