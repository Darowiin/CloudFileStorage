package ru.config;

import io.minio.MinioClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class TestContainersConfig {

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    public static final GenericContainer<?> REDIS = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    public static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2025-04-22T22-12-26Z")
            .withUserName("minioTest")
            .withPassword("minioTestPass");

    static {
        POSTGRES.start();
        REDIS.start();
        MINIO.start();

        System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES.getPassword());

        System.setProperty("spring.data.redis.host", REDIS.getHost());
        System.setProperty("spring.data.redis.port", String.valueOf(REDIS.getMappedPort(6379)));

        System.setProperty("minio.endpoint", MINIO.getS3URL());
        System.setProperty("minio.access-key", MINIO.getUserName());
        System.setProperty("minio.secret-key", MINIO.getPassword());

        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        System.setProperty("spring.liquibase.enabled", "false");
    }

    @Bean
    @Primary
    public MinioClient testMinioClient() {
        return MinioClient.builder()
                .endpoint(MINIO.getS3URL())
                .credentials(MINIO.getUserName(), MINIO.getPassword())
                .build();
    }
}
