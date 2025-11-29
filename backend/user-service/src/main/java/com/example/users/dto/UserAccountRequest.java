package com.example.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserAccountRequest(
        @NotBlank String subject,
        @NotBlank String username,
        @Email String email,
        @NotEmpty Set<String> roles
) {
}


