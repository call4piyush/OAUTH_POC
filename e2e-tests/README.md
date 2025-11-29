# E2E Tests

End-to-end tests for the OAuth2 microservices architecture.

## Prerequisites

- Docker and Docker Compose
- All services running via `docker compose up`

## Running Tests

```bash
# Start all services
docker compose up -d

# Wait for services to be ready
sleep 30

# Run E2E tests
mvn test

# Or run specific test class
mvn test -Dtest=OAuth2E2ETest
```

## Test Coverage

- Keycloak accessibility and configuration
- API Gateway health and JWT validation
- User Service health and RBAC enforcement
- BFF authentication flow
- End-to-end OAuth2 flow

## Notes

- Tests assume services are running on default ports
- Some tests require users to be created in Keycloak first
- Tests use RestAssured for HTTP testing

