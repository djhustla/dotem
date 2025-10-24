# Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copier le JAR
COPY target/security-learning-1.0.0.jar app.jar

# Port exposé (Render utilisera $PORT)
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]