package com.example.users.dto;

import java.time.Instant;
import java.util.Set;

public record UserAccountResponse(
        Long id,
        String subject,
        String username,
        String email,
        Set<String> roles,
        Instant createdAt
) {
}


