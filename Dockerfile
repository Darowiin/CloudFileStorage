FROM openjdk:21-slim

WORKDIR /app

COPY target/cloud-file-storage-*.jar app.jar
COPY .env /app/.env

EXPOSE 8080

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/file_storage
ENV SPRING_DATASOURCE_USERNAME=root
ENV SPRING_DATASOURCE_PASSWORD=123
ENV SPRING_DATA_REDIS_HOST=redis
ENV SPRING_DATA_REDIS_PORT=6379
ENV MINIO_ENDPOINT=http://minio:9000

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
