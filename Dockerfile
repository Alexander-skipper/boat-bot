FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

FROM amazoncorretto:21

WORKDIR /app

COPY --from=builder /app/target/app.jar app.jar

RUN ls -la /app
RUN java -version
RUN jar tf app.jar | grep -i "BotApplication" || echo "Main class not found"
RUN unzip -p app.jar META-INF/MANIFEST.MF || echo "No manifest"

CMD ["java", "-jar", "app.jar"]