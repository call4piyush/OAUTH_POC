package com.example.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("OAuth2 E2E Tests")
class OAuth2E2ETest {

    private static final String KEYCLOAK_BASE_URL = "http://localhost:8080";
    private static final String GATEWAY_URL = "http://localhost:8082";
    private static final String BFF_URL = "http://localhost:8081";
    private static final String REALM = "poc";
    private static final String CLIENT_ID = "bff-client";

    @BeforeAll
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @DisplayName("Should verify Keycloak is accessible")
    void shouldVerifyKeycloakIsAccessible() {
        given()
                .baseUri(KEYCLOAK_BASE_URL)
                .when()
                .get("/realms/" + REALM)
                .then()
                .statusCode(200)
                .body("realm", equalTo(REALM));
    }

    @Test
    @DisplayName("Should verify API Gateway health endpoint")
    void shouldVerifyApiGatewayHealthEndpoint() {
        given()
                .baseUri(GATEWAY_URL)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Should verify User Service health endpoint")
    void shouldVerifyUserServiceHealthEndpoint() {
        given()
                .baseUri("http://localhost:8083")
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Should reject unauthenticated requests to protected endpoints")
    void shouldRejectUnauthenticatedRequestsToProtectedEndpoints() {
        given()
                .baseUri(GATEWAY_URL)
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should verify BFF login endpoint redirects to Keycloak")
    void shouldVerifyBffLoginEndpointRedirectsToKeycloak() {
        given()
                .baseUri(BFF_URL)
                .redirects()
                .follow(false)
                .when()
                .get("/auth/login")
                .then()
                .statusCode(302)
                .header("Location", containsString(KEYCLOAK_BASE_URL))
                .header("Location", containsString("protocol/openid-connect/auth"));
    }

    @Test
    @DisplayName("Should verify BFF session endpoint requires authentication")
    void shouldVerifyBffSessionEndpointRequiresAuthentication() {
        given()
                .baseUri(BFF_URL)
                .when()
                .get("/session/me")
                .then()
                .statusCode(401)
                .body("error", equalTo("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Should verify Keycloak realm configuration")
    void shouldVerifyKeycloakRealmConfiguration() {
        var response = given()
                .baseUri(KEYCLOAK_BASE_URL)
                .when()
                .get("/realms/" + REALM)
                .then()
                .statusCode(200)
                .extract()
                .response();

        assertThat(response.jsonPath().getString("realm")).isEqualTo(REALM);
        assertThat(response.jsonPath().getBoolean("enabled")).isTrue();
    }

    @Test
    @DisplayName("Should verify Keycloak client configuration")
    void shouldVerifyKeycloakClientConfiguration() {
        var response = given()
                .baseUri(KEYCLOAK_BASE_URL)
                .when()
                .get("/realms/" + REALM + "/clients")
                .then()
                .statusCode(200)
                .extract()
                .response();

        var clients = response.jsonPath().getList("$");
        assertThat(clients).isNotEmpty();
        
        var bffClient = clients.stream()
                .filter(client -> CLIENT_ID.equals(((java.util.Map<?, ?>) client).get("clientId")))
                .findFirst();
        
        assertThat(bffClient).isPresent();
    }
}

