FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

RUN ls -la /app/target/

RUN mkdir -p /app/artifacts && cp /app/target/*.jar /app/artifacts/app.jar

FROM amazoncorretto:21-alpine

WORKDIR /app
COPY --from=builder /app/artifacts/app.jar app.jar

RUN ls -la /app/

CMD ["java", "-jar", "app.jar"]