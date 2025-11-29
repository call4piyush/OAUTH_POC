package com.example.users.service;

import com.example.users.domain.UserAccount;
import com.example.users.dto.UserAccountRequest;
import com.example.users.dto.UserAccountResponse;
import com.example.users.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repository;

    @Transactional(readOnly = true)
    public List<UserAccountResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(UserAccountService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserAccountResponse getBySubject(String subject) {
        return repository.findBySubject(subject)
                .map(UserAccountService::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Subject not registered: " + subject));
    }

    @Transactional
    public UserAccountResponse upsert(UserAccountRequest request) {
        UserAccount account = repository.findBySubject(request.subject())
                .map(existing -> {
                    existing.setUsername(request.username());
                    existing.setEmail(request.email());
                    existing.setRoles(request.roles());
                    return existing;
                })
                .orElseGet(() -> UserAccount.builder()
                        .subject(request.subject())
                        .username(request.username())
                        .email(request.email())
                        .roles(request.roles())
                        .build());
        return toResponse(repository.save(account));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private static UserAccountResponse toResponse(UserAccount account) {
        return new UserAccountResponse(
                account.getId(),
                account.getSubject(),
                account.getUsername(),
                account.getEmail(),
                account.getRoles(),
                account.getCreatedAt()
        );
    }
}


