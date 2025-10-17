
# Build stage
FROM maven:3.8-openjdk-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-slim
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080

# Définis le profil PROD correctement
ENV SPRING_PROFILES_ACTIVE=prod

CMD ["java", "-jar", "app.jar"]