FROM maven:3.8.6-openjdk-17 as builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-slim
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]