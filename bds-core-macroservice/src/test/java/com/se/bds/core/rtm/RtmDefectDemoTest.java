package com.se.bds.core.rtm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Demo test class to trigger RTM defect generation without requiring Docker/Spring Context.
 */
@DisplayName("RTM Defect Generation Demo")
public class RtmDefectDemoTest {

    @Test
    @RtmTestCase(id = "TC-2.2.1-19")
    @DisplayName("TC-2.2.1-19: Verify property filtering by multiple statuses")
    void test_TC_2_2_1_19() {
        fail("Property filtering by multiple statuses returned incorrect results: Expected 5 properties, but got 3. " +
                "Potential issue in search query construction when handling multiple status values.");
    }

    @Test
    @RtmTestCase(id = "TC-2.1.1-04")
    @DisplayName("TC-2.1.1-04: Verify registration fails when email already exists")
    void test_TC_2_1_1_04() {
        fail("User registration allowed duplicate emails. Expected 400 Bad Request but received 200 OK for an existing email address.");
    }

    @Test
    @RtmTestCase(id = "TC-4.1.1-01")
    @DisplayName("TC-4.1.1-01: Verify JWT token signature validation")
    void test_TC_4_1_1_01() {
        fail("System accepted a JWT token with an invalid signature. Security vulnerability: tokens should be strictly validated against the public key.");
    }
}
