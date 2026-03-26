FROM maven:3.9-amazoncorretto-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app
COPY --from=builder /app/target/boat-bot-1.0-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "app.jar"]