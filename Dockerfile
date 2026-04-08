FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

RUN ls -la /app/target


FROM amazoncorretto:21-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN ls -la /app

CMD ["java", "-jar", "app.jar"]