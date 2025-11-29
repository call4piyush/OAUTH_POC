package com.example.users.integration;

import com.example.users.domain.UserAccount;
import com.example.users.dto.UserAccountRequest;
import com.example.users.dto.UserAccountResponse;
import com.example.users.repository.UserAccountRepository;
import com.example.users.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserAccountService.class)
@ActiveProfiles("test")
@DisplayName("UserAccount Integration Tests")
class UserAccountIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should persist and retrieve user account with Hibernate")
    void shouldPersistAndRetrieveUserAccount() {
        // Given
        UserAccountRequest request = new UserAccountRequest(
                "test-subject",
                "testuser",
                "test@example.com",
                Set.of("ROLE_USER", "ROLE_ADMIN")
        );

        // When
        UserAccountResponse saved = service.upsert(request);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(saved.id()).isNotNull();
        assertThat(saved.subject()).isEqualTo("test-subject");

        UserAccountResponse found = service.getBySubject("test-subject");
        assertThat(found).isNotNull();
        assertThat(found.username()).isEqualTo("testuser");
        assertThat(found.email()).isEqualTo("test@example.com");
        assertThat(found.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should update existing user account")
    void shouldUpdateExistingUserAccount() {
        // Given
        UserAccountRequest initial = new UserAccountRequest(
                "test-subject",
                "olduser",
                "old@example.com",
                Set.of("ROLE_USER")
        );
        service.upsert(initial);
        entityManager.flush();
        entityManager.clear();

        // When
        UserAccountRequest update = new UserAccountRequest(
                "test-subject",
                "newuser",
                "new@example.com",
                Set.of("ROLE_ADMIN")
        );
        UserAccountResponse updated = service.upsert(update);
        entityManager.flush();
        entityManager.clear();

        // Then
        UserAccountResponse found = service.getBySubject("test-subject");
        assertThat(found.username()).isEqualTo("newuser");
        assertThat(found.email()).isEqualTo("new@example.com");
        assertThat(found.roles()).containsExactly("ROLE_ADMIN");
        assertThat(found.id()).isEqualTo(updated.id()); // Same ID
    }

    @Test
    @DisplayName("Should handle element collection for roles")
    void shouldHandleElementCollectionForRoles() {
        // Given
        UserAccountRequest request = new UserAccountRequest(
                "test-subject",
                "testuser",
                "test@example.com",
                Set.of("ROLE_USER", "ROLE_ADMIN", "ROLE_MANAGER")
        );

        // When
        service.upsert(request);
        entityManager.flush();
        entityManager.clear();

        // Then
        UserAccount entity = repository.findBySubject("test-subject").orElseThrow();
        assertThat(entity.getRoles()).hasSize(3);
        assertThat(entity.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_MANAGER");
    }

    @Test
    @DisplayName("Should delete user account")
    void shouldDeleteUserAccount() {
        // Given
        UserAccountRequest request = new UserAccountRequest(
                "test-subject",
                "testuser",
                "test@example.com",
                Set.of("ROLE_USER")
        );
        UserAccountResponse saved = service.upsert(request);
        entityManager.flush();
        entityManager.clear();

        // When
        service.delete(saved.id());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(repository.findBySubject("test-subject")).isEmpty();
        assertThat(repository.findById(saved.id())).isEmpty();
    }

    @Test
    @DisplayName("Should find all users")
    void shouldFindAllUsers() {
        // Given
        service.upsert(new UserAccountRequest("subject-1", "user1", "user1@example.com", Set.of("ROLE_USER")));
        service.upsert(new UserAccountRequest("subject-2", "user2", "user2@example.com", Set.of("ROLE_ADMIN")));
        service.upsert(new UserAccountRequest("subject-3", "user3", "user3@example.com", Set.of("ROLE_USER")));
        entityManager.flush();
        entityManager.clear();

        // When
        var all = service.findAll();

        // Then
        assertThat(all).hasSize(3);
    }
}

