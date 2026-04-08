FROM maven:3.9-amazoncorretto-21

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Запускаем shaded JAR напрямую
CMD ["java", "-jar", "target/boat-bot-1.0-SNAPSHOT-shaded.jar"]