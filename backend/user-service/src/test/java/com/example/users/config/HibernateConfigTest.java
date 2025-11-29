package com.example.users.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Hibernate Configuration Tests")
class HibernateConfigTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Should configure HikariCP connection pool")
    void shouldConfigureHikariCpConnectionPool() {
        // Then
        assertThat(dataSource).isNotNull();
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertThat(hikariDataSource.getMaximumPoolSize()).isGreaterThan(0);
        assertThat(hikariDataSource.getMinimumIdle()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should have valid connection")
    void shouldHaveValidConnection() throws Exception {
        // When
        try (var connection = dataSource.getConnection()) {
            // Then
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(5)).isTrue();
        }
    }
}

