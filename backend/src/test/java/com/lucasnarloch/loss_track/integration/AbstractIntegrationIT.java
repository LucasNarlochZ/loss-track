package com.lucasnarloch.loss_track.integration;

import com.lucasnarloch.loss_track.LossTrackApplication;
import com.lucasnarloch.loss_track.domain.auth.RegistrationService;
import com.lucasnarloch.loss_track.domain.auth.dtos.RegisterRequestDTO;
import com.lucasnarloch.loss_track.domain.user.User;
import com.lucasnarloch.loss_track.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LossTrackApplication.class, properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "security.cookie.secure=true",
        "spring.jpa.open-in-view=false",
        "spring.jpa.show-sql=false",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@Import(AbstractIntegrationIT.Containers.class)
abstract class AbstractIntegrationIT {
    protected static final String CNPJ = "12345678000190";
    protected static final String OTHER_CNPJ = "98765432000110";
    protected static final String EMAIL = "owner@example.com";
    protected static final String PASSWORD = "integration-password";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper json;

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected StringRedisTemplate redis;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected RegistrationService registration;

    @BeforeEach
    void cleanStorage() {
        jdbc.sql("DELETE FROM products").update();
        jdbc.sql("DELETE FROM stores").update();
        jdbc.sql("DELETE FROM users").update();
        jdbc.sql("DELETE FROM tenants").update();
        try (var connection = redis.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    protected User register(String cnpj) {
        registration.register(new RegisterRequestDTO("Integration tenant", cnpj, "Owner", EMAIL, PASSWORD));
        return users.findByTenantCnpjAndEmail(cnpj, EMAIL).orElseThrow();
    }

    protected MvcResult login(String cnpj, String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("cnpj", cnpj, "email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    protected String accessToken(String cnpj) throws Exception {
        return body(login(cnpj, EMAIL, PASSWORD)).path("accessToken").asString();
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:16");
        }

        @Bean
        @ServiceConnection(name = "redis")
        GenericContainer<?> redisContainer() {
            return new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
        }
    }
}
