package com.example.users.integration;

import com.example.users.domain.UserAccount;
import com.example.users.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UserAccountController Integration Tests")
class UserAccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository repository;


    private Jwt userJwt;
    private Jwt adminJwt;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        userJwt = Jwt.withTokenValue("user-token")
                .header("alg", "RS256")
                .claim("sub", "user-subject")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ROLE_USER")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        adminJwt = Jwt.withTokenValue("admin-token")
                .header("alg", "RS256")
                .claim("sub", "admin-subject")
                .claim("realm_access", java.util.Map.of("roles", java.util.List.of("ROLE_ADMIN")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Create test user account
        repository.save(UserAccount.builder()
                .subject("user-subject")
                .username("testuser")
                .email("test@example.com")
                .roles(Set.of("ROLE_USER"))
                .build());
    }

    @Test
    @DisplayName("Should get current user profile with valid JWT")
    void shouldGetCurrentUserProfileWithValidJwt() throws Exception {
        mockMvc.perform(get("/users/me")
                        .with(jwt().jwt(userJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("user-subject"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Should return 401 for unauthenticated request")
    void shouldReturn401ForUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should create user account as admin")
    void shouldCreateUserAccountAsAdmin() throws Exception {
        String requestBody = """
                {
                    "subject": "new-subject",
                    "username": "newuser",
                    "email": "new@example.com",
                    "roles": ["ROLE_USER"]
                }
                """;

        mockMvc.perform(post("/users")
                        .with(jwt().jwt(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("new-subject"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should return 403 when user tries to create account")
    void shouldReturn403WhenUserTriesToCreateAccount() throws Exception {
        String requestBody = """
                {
                    "subject": "new-subject",
                    "username": "newuser",
                    "email": "new@example.com",
                    "roles": ["ROLE_USER"]
                }
                """;

        mockMvc.perform(post("/users")
                        .with(jwt().jwt(userJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should get all users as admin")
    void shouldGetAllUsersAsAdmin() throws Exception {
        // Create another user
        repository.save(UserAccount.builder()
                .subject("another-subject")
                .username("anotheruser")
                .email("another@example.com")
                .roles(Set.of("ROLE_USER"))
                .build());

        mockMvc.perform(get("/users")
                        .with(jwt().jwt(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should delete user as admin")
    void shouldDeleteUserAsAdmin() throws Exception {
        UserAccount toDelete = repository.save(UserAccount.builder()
                .subject("delete-subject")
                .username("deleteuser")
                .email("delete@example.com")
                .roles(Set.of("ROLE_USER"))
                .build());

        mockMvc.perform(delete("/users/" + toDelete.getId())
                        .with(jwt().jwt(adminJwt)))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertThat(repository.findById(toDelete.getId())).isEmpty();
    }
}

