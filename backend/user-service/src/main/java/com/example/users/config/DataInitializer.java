package com.example.users.config;

import com.example.users.domain.UserAccount;
import com.example.users.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserAccountRepository repository;

    @Bean
    CommandLineRunner seedAdmin() {
        return args -> repository.findBySubject("admin-subject").orElseGet(() -> repository.save(
                UserAccount.builder()
                        .subject("admin-subject")
                        .username("admin")
                        .email("admin@example.com")
                        .roles(Set.of("ROLE_ADMIN"))
                        .build()
        ));
    }
}

