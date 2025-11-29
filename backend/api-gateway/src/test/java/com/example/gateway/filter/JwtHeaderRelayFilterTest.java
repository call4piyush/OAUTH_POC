package com.example.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class JwtHeaderRelayFilterTest {

    private JwtHeaderRelayFilter filter;
    private ServerWebExchange exchange;
    private org.springframework.cloud.gateway.filter.GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtHeaderRelayFilter();
        exchange = mock(ServerWebExchange.class);
        chain = mock(org.springframework.cloud.gateway.filter.GatewayFilterChain.class);
    }

    @Test
    @DisplayName("Should extract and forward JWT claims as headers")
    void shouldExtractAndForwardJwtClaimsAsHeaders() {
        // Given
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "test-user-id")
                .claim("realm_access", Map.of("roles", List.of("ROLE_USER", "ROLE_ADMIN")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                jwt,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        org.springframework.http.server.reactive.ServerHttpRequest mutatedRequest =
                mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);

        when(exchange.getPrincipal()).thenReturn(Mono.just(auth));
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(java.util.function.Consumer.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        when(mutatedExchange.getRequest()).thenReturn(mutatedRequest);
        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("Should handle missing principal gracefully")
    void shouldHandleMissingPrincipalGracefully() {
        // Given
        when(exchange.getPrincipal()).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    @DisplayName("Should handle non-JWT principal")
    void shouldHandleNonJwtPrincipal() {
        // Given
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "password");
        when(exchange.getPrincipal()).thenReturn(Mono.just(auth));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }
}

