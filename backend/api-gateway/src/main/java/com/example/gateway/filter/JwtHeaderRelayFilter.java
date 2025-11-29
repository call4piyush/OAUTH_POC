package com.example.gateway.filter;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;

/**
 * Extracts JWT claims and roles, forwarding them as headers to downstream services.
 */
@Component
public class JwtHeaderRelayFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(AbstractAuthenticationToken.class)
                .flatMap(auth -> {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof Jwt jwt) {
                        ServerWebExchange mutated = exchange.mutate()
                                .request(builder -> builder.headers(httpHeaders -> {
                                    httpHeaders.set(USER_ID_HEADER, jwt.getSubject());
                                    httpHeaders.set(USER_ROLES_HEADER, serializeAuthorities(auth.getAuthorities()));
                                }))
                                .build();
                        return chain.filter(mutated);
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private String serializeAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }

    @Override
    public int getOrder() {
        return -20;
    }
}

