# TP5 — image exécutable du backend Spring Boot (build reproductible dans le conteneur).
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY authentification_back ./authentification_back
COPY authentification_front ./authentification_front

RUN mvn -B -f pom.xml -pl authentification_back -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /workspace/authentification_back/target/authentification_back-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
