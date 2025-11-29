package com.example.users.web;

import com.example.users.dto.UserAccountRequest;
import com.example.users.dto.UserAccountResponse;
import com.example.users.service.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService service;

    @GetMapping
    public List<UserAccountResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/me")
    public UserAccountResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return service.getBySubject(jwt.getSubject());
    }

    @PostMapping
    public ResponseEntity<UserAccountResponse> upsert(@RequestBody @Valid UserAccountRequest request) {
        UserAccountResponse response = service.upsert(request);
        return ResponseEntity
                .created(URI.create("/users/" + response.id()))
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}


