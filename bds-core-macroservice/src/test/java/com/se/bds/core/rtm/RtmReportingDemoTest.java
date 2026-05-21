package com.se.bds.core.rtm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RTM Reporting Demo")
class RtmReportingDemoTest {

    @Test
    @RtmTestCase(id = "TC-2.1.1-01")
    @DisplayName("Verify User Registration Success")
    void testUserRegistrationSuccess() {
        // Simulate a passing test for user registration
        assertTrue(true, "User should be registered successfully");
    }

    @Test
    @RtmTestCase(id = "TC-2.1.1-02")
    @DisplayName("Verify Login Failure with Invalid Credentials")
    void testLoginFailureInvalidCredentials() {
        // Simulate a failing test to demonstrate defect generation
        String expectedMessage = "Invalid credentials";
        String actualMessage = "Login successful"; // Simulated bug
        
        assertEquals(expectedMessage, actualMessage, "Should return error message for invalid login");
    }

    @Test
    @RtmTestCase(id = "TC-2.2.2-05")
    @DisplayName("Verify Add Property to Favorites")
    void testAddPropertyToFavorites() {
        // Another passing test from a different requirement category
        boolean isLiked = true;
        assertTrue(isLiked, "Property should be marked as favorite");
    }
}
