# Testing Guide

This document describes the testing strategy and how to run tests for the OAuth2 microservices architecture.

## Test Structure

### Unit Tests
- **Location**: `backend/*/src/test/java`
- **Purpose**: Test individual components in isolation
- **Coverage**:
  - Service layer logic
  - Controller endpoints with mocked dependencies
  - Repository methods
  - Gateway filters

### Integration Tests
- **Location**: `backend/user-service/src/test/java/com/example/users/integration`
- **Purpose**: Test components with real database (H2 in-memory)
- **Coverage**:
  - Full request/response cycle
  - Database persistence
  - Hibernate entity mapping
  - Security integration

### E2E Tests
- **Location**: `e2e-tests/src/test/java`
- **Purpose**: Test complete system with all services running
- **Coverage**:
  - OAuth2 flow
  - Service communication
  - Gateway routing
  - RBAC enforcement

## Running Tests

### Unit and Integration Tests

```bash
# Run all tests for user-service
cd backend/user-service
mvn test

# Run all tests for api-gateway
cd backend/api-gateway
mvn test

# Run specific test class
mvn test -Dtest=UserAccountServiceTest

# Run with coverage
mvn test jacoco:report
```

### E2E Tests

```bash
# Start all services
docker compose up -d

# Wait for services to be ready
sleep 30

# Run E2E tests
cd e2e-tests
mvn test

# Run specific E2E test
mvn test -Dtest=OAuth2E2ETest
```

## Test Configuration

### Test Profiles
- **test**: Uses H2 in-memory database
- **docker**: Uses PostgreSQL (for Docker Compose)

### Test Dependencies
- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- Spring Security Test
- Testcontainers (for E2E)

## Test Coverage

### User Service
- ✅ UserAccountService (unit)
- ✅ UserAccountController (unit + integration)
- ✅ UserAccountRepository (integration)
- ✅ Hibernate configuration (integration)
- ✅ Security configuration (integration)

### API Gateway
- ✅ JwtHeaderRelayFilter (unit)
- ✅ Security configuration (integration)

### E2E
- ✅ Keycloak connectivity
- ✅ Service health checks
- ✅ Authentication flow
- ✅ RBAC enforcement

## Writing New Tests

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;
    
    @InjectMocks
    private MyService service;
    
    @Test
    void shouldDoSomething() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        
        // When
        var result = service.findById(1L);
        
        // Then
        assertThat(result).isNotNull();
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldHandleRequest() throws Exception {
        mockMvc.perform(get("/endpoint")
                .with(jwt().jwt(jwt)))
            .andExpect(status().isOk());
    }
}
```

## Continuous Integration

Tests should be run in CI/CD pipeline:
1. Unit tests (fast, no dependencies)
2. Integration tests (requires database)
3. E2E tests (requires all services)

## Troubleshooting

### Tests failing with database errors
- Ensure test profile is active
- Check H2 dependency is present
- Verify `application-test.yml` exists

### Security tests failing
- Ensure `spring-security-test` dependency is present
- Check JWT mock configuration
- Verify security configuration is correct

### E2E tests failing
- Ensure all services are running
- Check service ports are correct
- Verify Keycloak realm is imported
- Wait for services to be fully ready

