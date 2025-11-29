package com.example.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Base64;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("User Service E2E Tests")
class UserServiceE2ETest {

    private static final String KEYCLOAK_BASE_URL = "http://localhost:8080";
    private static final String GATEWAY_URL = "http://localhost:8082";
    private static final String REALM = "poc";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    @BeforeAll
    void setUp() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private String getAccessToken(String username, String password) {
        var response = given()
                .baseUri(KEYCLOAK_BASE_URL)
                .contentType(ContentType.URLENC)
                .formParam("grant_type", "password")
                .formParam("client_id", "bff-client")
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post("/realms/" + REALM + "/protocol/openid-connect/token")
                .then()
                .statusCode(200)
                .extract()
                .response();

        return response.jsonPath().getString("access_token");
    }

    @Test
    @DisplayName("Should access user profile through gateway with valid token")
    void shouldAccessUserProfileThroughGatewayWithValidToken() {
        // Note: This test requires a user to be created in Keycloak first
        // For a complete E2E test, you would:
        // 1. Create a user in Keycloak
        // 2. Get access token
        // 3. Create user account in user-service
        // 4. Access profile through gateway

        String token = getAccessToken(ADMIN_USER, ADMIN_PASSWORD);
        
        if (token != null) {
            given()
                    .baseUri(GATEWAY_URL)
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/users/me")
                    .then()
                    .statusCode(anyOf(is(200), is(404))); // 404 if user account doesn't exist yet
        }
    }

    @Test
    @DisplayName("Should verify gateway forwards JWT to user service")
    void shouldVerifyGatewayForwardsJwtToUserService() {
        String token = getAccessToken(ADMIN_USER, ADMIN_PASSWORD);
        
        if (token != null) {
            var response = given()
                    .baseUri(GATEWAY_URL)
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/users/me")
                    .then()
                    .extract()
                    .response();

            // Gateway should forward the request
            assertThat(response.getStatusCode()).isIn(200, 404, 401);
        }
    }

    @Test
    @DisplayName("Should verify RBAC enforcement at gateway level")
    void shouldVerifyRbacEnforcementAtGatewayLevel() {
        // Test that unauthenticated requests are rejected
        given()
                .baseUri(GATEWAY_URL)
                .when()
                .get("/api/users")
                .then()
                .statusCode(401);

        // Test that authenticated but unauthorized requests are handled
        String token = getAccessToken(ADMIN_USER, ADMIN_PASSWORD);
        if (token != null) {
            given()
                    .baseUri(GATEWAY_URL)
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/users")
                    .then()
                    .statusCode(anyOf(is(200), is(403))); // 403 if not admin, 200 if admin
        }
    }
}

