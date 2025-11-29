package com.example.users.service;

import com.example.users.domain.UserAccount;
import com.example.users.dto.UserAccountRequest;
import com.example.users.dto.UserAccountResponse;
import com.example.users.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountService Unit Tests")
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository repository;

    @InjectMocks
    private UserAccountService service;

    private UserAccount testUser;
    private UserAccountRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = UserAccount.builder()
                .id(1L)
                .subject("test-subject")
                .username("testuser")
                .email("test@example.com")
                .roles(Set.of("ROLE_USER"))
                .createdAt(Instant.now())
                .build();

        testRequest = new UserAccountRequest(
                "test-subject",
                "testuser",
                "test@example.com",
                Set.of("ROLE_USER")
        );
    }

    @Test
    @DisplayName("Should find all users")
    void shouldFindAllUsers() {
        // Given
        UserAccount user2 = UserAccount.builder()
                .id(2L)
                .subject("subject-2")
                .username("user2")
                .email("user2@example.com")
                .roles(Set.of("ROLE_ADMIN"))
                .createdAt(Instant.now())
                .build();

        when(repository.findAll()).thenReturn(List.of(testUser, user2));

        // When
        List<UserAccountResponse> result = service.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).subject()).isEqualTo("test-subject");
        assertThat(result.get(1).subject()).isEqualTo("subject-2");
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find user by subject")
    void shouldFindUserBySubject() {
        // Given
        when(repository.findBySubject("test-subject")).thenReturn(Optional.of(testUser));

        // When
        UserAccountResponse result = service.getBySubject("test-subject");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.subject()).isEqualTo("test-subject");
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.roles()).contains("ROLE_USER");
        verify(repository, times(1)).findBySubject("test-subject");
    }

    @Test
    @DisplayName("Should throw exception when subject not found")
    void shouldThrowExceptionWhenSubjectNotFound() {
        // Given
        when(repository.findBySubject("unknown-subject")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getBySubject("unknown-subject"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subject not registered: unknown-subject");
        verify(repository, times(1)).findBySubject("unknown-subject");
    }

    @Test
    @DisplayName("Should create new user when upserting non-existent user")
    void shouldCreateNewUserWhenUpsertingNonExistentUser() {
        // Given
        when(repository.findBySubject("test-subject")).thenReturn(Optional.empty());
        when(repository.save(any(UserAccount.class))).thenReturn(testUser);

        // When
        UserAccountResponse result = service.upsert(testRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.subject()).isEqualTo("test-subject");
        verify(repository, times(1)).findBySubject("test-subject");
        verify(repository, times(1)).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("Should update existing user when upserting")
    void shouldUpdateExistingUserWhenUpserting() {
        // Given
        UserAccountRequest updateRequest = new UserAccountRequest(
                "test-subject",
                "updateduser",
                "updated@example.com",
                Set.of("ROLE_ADMIN", "ROLE_USER")
        );

        when(repository.findBySubject("test-subject")).thenReturn(Optional.of(testUser));
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            assertThat(saved.getUsername()).isEqualTo("updateduser");
            assertThat(saved.getEmail()).isEqualTo("updated@example.com");
            assertThat(saved.getRoles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
            return saved;
        });

        // When
        UserAccountResponse result = service.upsert(updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("updateduser");
        assertThat(result.email()).isEqualTo("updated@example.com");
        verify(repository, times(1)).findBySubject("test-subject");
        verify(repository, times(1)).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("Should delete user by id")
    void shouldDeleteUserById() {
        // Given
        doNothing().when(repository).deleteById(1L);

        // When
        service.delete(1L);

        // Then
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should map entity to response correctly")
    void shouldMapEntityToResponseCorrectly() {
        // Given
        when(repository.findBySubject("test-subject")).thenReturn(Optional.of(testUser));

        // When
        UserAccountResponse result = service.getBySubject("test-subject");

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.subject()).isEqualTo("test-subject");
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.roles()).isEqualTo(Set.of("ROLE_USER"));
        assertThat(result.createdAt()).isNotNull();
    }
}

