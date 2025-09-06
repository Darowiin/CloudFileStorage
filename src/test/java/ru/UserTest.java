package ru;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.config.TestContainersConfig;
import ru.repository.UserRepository;
import ru.service.UserService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(TestContainersConfig.class)
class UserTest {

    private final UserService userService;
    private final MockMvc mockMvc;
    private final UserRepository userRepository;

    @BeforeEach
    void flushRedis() throws Exception {
        TestContainersConfig.REDIS.execInContainer("redis-cli", "FLUSHALL");
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

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

        ExecResult result = TestContainersConfig.REDIS.execInContainer("redis-cli", "keys", "*");
        Assertions.assertTrue(result.getStdout().contains("spring:session:sessions"));
    }

    @Test
    void validationError_ReturnsBadRequest() throws Exception {
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
    void signOut_WithAuthenticatedUser_RemovesSessionFromRedis() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"password\"}")
                        .session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"password\"}")
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/sign-out")
                        .with(user("user").roles("USER"))
                        .session(session))
                .andExpect(status().isNoContent());
    }


    @Test
    void signOut_WithoutAuthenticatedUser_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUser_WithAuthenticatedUser_ReturnsOk() throws Exception {

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password\"}"))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        String sessionCookie = response.getCookie("SESSION").getValue();

        mockMvc.perform(get("/api/user/me")
                        .cookie(new Cookie("SESSION", sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"username\":\"testuser\"}"));
        ExecResult redisResult = TestContainersConfig.REDIS.execInContainer("redis-cli", "keys", "*");
        Assertions.assertTrue(redisResult.getStdout().contains("spring:session:sessions"));
    }
}
