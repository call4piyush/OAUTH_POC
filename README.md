## OAuth2/OIDC Microservices BFF POC

This proof-of-concept demonstrates a microservices stack that uses a Backend-for-Frontend (BFF) gateway and a centralized Keycloak Identity Provider. Users authenticate via OAuth2/OIDC and obtain JWT access tokens that the API Gateway validates before forwarding requests to downstream microservices. Services are dockerized and orchestrated through `docker compose`.

### Components

- `infra/keycloak`: Keycloak plus backing Postgres database seeded with realms, clients, and roles for RBAC.
- `backend/api-gateway`: Spring Cloud Gateway acting as the microservice edge. It validates JWTs with Keycloak, handles RBAC via scopes/roles, and forwards whitelisted headers to downstream services.
- `backend/user-service`: Spring Boot service with Hibernate/JPA auto-configuration, Postgres storage, and role-protected REST resources.
- `services/bff`: Lightweight Node/TypeScript BFF that handles browser authentication (PKCE flow), securely stores refresh tokens, exchanges them for access tokens, and moderates API calls.
- `frontend/auth-ui`: Angular UI that talks only to the BFF and never holds long-lived tokens.

### High-Level Flow

1. Browser hits the Angular app which delegates login to the BFF.
2. BFF initiates PKCE login with Keycloak. After success, it stores refresh tokens server-side and sets an httpOnly session cookie.
3. BFF exchanges refresh tokens for JWT access tokens when needed and talks to the API Gateway.
4. API Gateway validates JWT signatures with the Keycloak JWKS endpoint. If valid, it forwards the request downstream with propagated headers (`X-User-Id`, `X-User-Roles`).
5. Microservices verify role requirements, perform business logic via Hibernate, and respond.

### Getting Started

```bash
cd /Users/piyushjoshi/Documents/POC/POC_OAUTH
docker compose up --build
```

Refer to `docs/SETUP.md` for detailed provisioning steps, Keycloak configuration, and Angular/BFF environment settings.

### Testing

The project includes comprehensive test coverage:

- **Unit Tests**: Test individual components in isolation (services, controllers, filters)
- **Integration Tests**: Test components with real database (H2 in-memory)
- **E2E Tests**: Test complete system with all services running

Run tests:

```bash
# Unit and integration tests
cd backend/user-service && mvn test
cd backend/api-gateway && mvn test

# E2E tests (requires services running)
docker compose up -d
cd e2e-tests && mvn test
```

See `TESTING.md` for detailed testing documentation.


