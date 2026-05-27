package com.se.bds.core.requirements;

import com.se.bds.core.BaseIntegrationTest;
import com.se.bds.core.rtm.RtmTestCase;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Req ID: 2.1.1
 * Feature Name: User Registration & Authentication
 * Actors: Guest, Customer, Property Owner, System
 */
@DisplayName("Req 2.1.1 - User Registration & Authentication")
class Req_2_1_1_Test extends BaseIntegrationTest {

    @Test
    @RtmTestCase(id = "TC-2.1.1-01")
    @DisplayName("TC-2.1.1-01: Verify Customer Registration Success")
    void testCustomerRegistrationSuccess() {
        // Implementation expected in future sprint
        /*
        given()
            .contentType(ContentType.JSON)
            .body("{ \"email\": \"new_customer@example.com\", \"password\": \"P@ssword123\", \"role\": \"CUSTOMER\" }")
        .when()
            .post("/public/auth/register")
        .then()
            .statusCode(200)
            .body("message", containsString("Registration successful"));
        */
    }

    @Test
    @RtmTestCase(id = "TC-2.1.1-02")
    @DisplayName("TC-2.1.1-02: Verify Login Failure with Invalid Credentials")
    void testLoginFailureInvalidCredentials() {
        /*
        given()
            .contentType(ContentType.JSON)
            .body("{ \"email\": \"wrong@example.com\", \"password\": \"wrong\" }")
        .when()
            .post("/public/auth/login")
        .then()
            .statusCode(401);
        */
    }

    @Test
    @RtmTestCase(id = "TC-2.1.1-03")
    @DisplayName("TC-2.1.1-03: Verify JWT Token Refresh")
    void testJwtTokenRefresh() {
        // Test logic here
    }

    // ... Methods for TC-2.1.1-04 to TC-2.1.1-15 ...
}
