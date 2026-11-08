# ---------- Étape 1 : construction (Maven + JDK 21) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
ARG APP_VERSION=dev
ARG GIT_COMMIT=local
RUN mvn -q -B -DskipTests package

# ---------- Étape 2 : image d'exécution minimale (JRE, utilisateur non root) ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S intervit && adduser -S intervit -G intervit
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
ARG APP_VERSION=dev
ARG GIT_COMMIT=local
ENV APP_VERSION=${APP_VERSION} GIT_COMMIT=${GIT_COMMIT} JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"
USER intervit
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
