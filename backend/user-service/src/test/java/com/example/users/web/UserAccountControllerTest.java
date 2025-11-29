package com.example.users.web;

import com.example.users.dto.UserAccountRequest;
import com.example.users.dto.UserAccountResponse;
import com.example.users.service.UserAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAccountController.class)
@DisplayName("UserAccountController Unit Tests")
class UserAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserAccountService service;

    private Jwt jwt;
    private UserAccountResponse testResponse;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "test-subject")
                .claim("realm_access", java.util.Map.of("roles", List.of("ROLE_USER")))
                .build();

        testResponse = new UserAccountResponse(
                1L,
                "test-subject",
                "testuser",
                "test@example.com",
                Set.of("ROLE_USER"),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should get current user profile")
    void shouldGetCurrentUserProfile() throws Exception {
        // Given
        when(service.getBySubject("test-subject")).thenReturn(testResponse);

        // When/Then
        mockMvc.perform(get("/users/me")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("test-subject"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(service, times(1)).getBySubject("test-subject");
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // When/Then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        verify(service, never()).getBySubject(anyString());
    }

    @Test
    @DisplayName("Should get all users for admin")
    void shouldGetAllUsersForAdmin() throws Exception {
        // Given
        Jwt adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "admin-subject")
                .claim("realm_access", java.util.Map.of("roles", List.of("ROLE_ADMIN")))
                .build();

        when(service.findAll()).thenReturn(List.of(testResponse));

        // When/Then
        mockMvc.perform(get("/users")
                        .with(jwt().jwt(adminJwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].subject").value("test-subject"));

        verify(service, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return 403 for non-admin accessing all users")
    void shouldReturn403ForNonAdminAccessingAllUsers() throws Exception {
        // When/Then
        mockMvc.perform(get("/users")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isForbidden());

        verify(service, never()).findAll();
    }

    @Test
    @DisplayName("Should create new user for admin")
    void shouldCreateNewUserForAdmin() throws Exception {
        // Given
        Jwt adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "admin-subject")
                .claim("realm_access", java.util.Map.of("roles", List.of("ROLE_ADMIN")))
                .build();

        UserAccountRequest request = new UserAccountRequest(
                "new-subject",
                "newuser",
                "new@example.com",
                Set.of("ROLE_USER")
        );

        UserAccountResponse response = new UserAccountResponse(
                2L,
                "new-subject",
                "newuser",
                "new@example.com",
                Set.of("ROLE_USER"),
                Instant.now()
        );

        when(service.upsert(any(UserAccountRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(post("/users")
                        .with(jwt().jwt(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("new-subject"))
                .andExpect(jsonPath("$.username").value("newuser"));

        verify(service, times(1)).upsert(any(UserAccountRequest.class));
    }

    @Test
    @DisplayName("Should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given
        Jwt adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "admin-subject")
                .claim("realm_access", java.util.Map.of("roles", List.of("ROLE_ADMIN")))
                .build();

        UserAccountRequest invalidRequest = new UserAccountRequest(
                "", // invalid: blank
                "",
                "invalid-email", // invalid: not an email
                Set.of()
        );

        // When/Then
        mockMvc.perform(post("/users")
                        .with(jwt().jwt(adminJwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(service, never()).upsert(any());
    }

    @Test
    @DisplayName("Should delete user for admin")
    void shouldDeleteUserForAdmin() throws Exception {
        // Given
        Jwt adminJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "admin-subject")
                .claim("realm_access", java.util.Map.of("roles", List.of("ROLE_ADMIN")))
                .build();

        doNothing().when(service).delete(1L);

        // When/Then
        mockMvc.perform(delete("/users/1")
                        .with(jwt().jwt(adminJwt)))
                .andExpect(status().isNoContent());

        verify(service, times(1)).delete(1L);
    }
}

