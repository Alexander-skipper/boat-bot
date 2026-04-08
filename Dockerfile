FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline --batch-mode

COPY src ./src
RUN mvn clean package -DskipTests --batch-mode
RUN ls -la /app/target/

FROM amazoncorretto:21

WORKDIR /app

COPY --from=builder /app/target/boat-bot-1.0-SNAPSHOT-shaded.jar app.jar
RUN ls -la /app/

CMD ["java", "-jar", "app.jar"]