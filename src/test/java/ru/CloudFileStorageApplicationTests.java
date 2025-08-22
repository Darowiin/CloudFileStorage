package ru;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.service.UserService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CloudFileStorageApplicationTests {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    private final UserService userService;
    private final MockMvc mockMvc;

    @Test
    void signUpAndSignIn_WorksCorrect() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user\",\"password\":\"password\"}")
                .session(session))
                .andExpectAll(
                    status().isCreated(),
                    content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                    content().json("{\"username\":\"user\"}")
                );

        Assertions.assertTrue(userService.existsByUsernameAndPassword("user", "password"));

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"password\"}")
                        .session(session))
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("{\"username\":\"user\"}")
                );
    }

    @Test
    void validationError_ReturnsCorrectCode() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"abc\",\"password\":\"password\"}")
                        .session(session))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("{\"message\":\"username size must be between 4 and 20\"}")
                );

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"abcd\",\"password\":\"pass\"}")
                        .session(session))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("{\"message\":\"password size must be between 6 and 20\"}")
                );

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"abc\",\"password\":\"password\"}")
                        .session(session))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("{\"message\":\"username size must be between 4 and 20\"}")
                );

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"abcd\",\"password\":\"pass\"}")
                        .session(session))
                .andExpectAll(
                        status().isBadRequest(),
                        content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                        content().json("{\"message\":\"password size must be between 6 and 20\"}")
                );
    }

    @Test
    @WithMockUser(username = "user")
    void signOut_WithMockUser_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isNoContent());
    }

}
