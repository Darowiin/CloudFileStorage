FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/cloud-file-storage-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
