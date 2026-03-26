# Этап 1: Сборка проекта
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск
FROM openjdk:21-jdk-slim

WORKDIR /app
COPY --from=builder /app/target/boat-bot-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]