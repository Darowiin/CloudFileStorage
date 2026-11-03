package ru.config;

import io.minio.MinioClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

@TestConfiguration
public class TestContainersConfig {

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    public static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.10.3-alpine")
            .withExposedPorts(6379);

    public static final GenericContainer<?> RUSTFS = new GenericContainer<>("rustfs/rustfs:1.0.4")
            .withExposedPorts(9000)
            .withEnv("RUSTFS_ACCESS_KEY", "rustfsTest")
            .withEnv("RUSTFS_SECRET_KEY", "rustfsTestPass")
            .withCommand("--console-enable", "/data")
            .waitingFor(new HttpWaitStrategy()
                    .forPort(9000)
                    .forPath("/health/ready"));

    static {
        POSTGRES.start();
        REDIS.start();
        RUSTFS.start();

        System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES.getPassword());

        System.setProperty("spring.data.redis.host", REDIS.getHost());
        System.setProperty("spring.data.redis.port", String.valueOf(REDIS.getMappedPort(6379)));

        System.setProperty("minio.endpoint", "http://" + RUSTFS.getHost() + ":" + RUSTFS.getMappedPort(9000));
        System.setProperty("minio.access-key", "rustfsTest");
        System.setProperty("minio.secret-key", "rustfsTestPass");

        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        System.setProperty("spring.liquibase.enabled", "false");
    }

    @Bean
    @Primary
    public MinioClient testMinioClient() {
        return MinioClient.builder()
                .endpoint("http://" + RUSTFS.getHost() + ":" + RUSTFS.getMappedPort(9000))
                .credentials("rustfsTest", "rustfsTestPass")
                .build();
    }
}
