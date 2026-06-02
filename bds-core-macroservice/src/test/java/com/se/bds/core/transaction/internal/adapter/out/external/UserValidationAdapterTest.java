package com.se.bds.core.transaction.internal.adapter.out.external;

import com.se.bds.common.exception.BusinessException;
import com.se.bds.common.message.validation.MSG12;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserValidationAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private UserValidationAdapter userValidationAdapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userValidationAdapter = new UserValidationAdapter(restTemplate);
        ReflectionTestUtils.setField(userValidationAdapter, "iamServiceUrl", "http://localhost:8081");
    }

    @Test
    void validateCustomer_Success() {
        UUID customerId = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("active", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(Map.class), any(Map.class)))
                .thenReturn(responseEntity);

        assertDoesNotThrow(() -> userValidationAdapter.validateCustomer(customerId));
    }

    @Test
    void validateCustomer_InactiveUser_ThrowsException() {
        UUID customerId = UUID.randomUUID();
        Map<String, Object> body = new HashMap<>();
        body.put("active", false);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(Map.class), any(Map.class)))
                .thenReturn(responseEntity);

        try {
            userValidationAdapter.validateCustomer(customerId);
            fail("Expected BusinessException to be thrown");
        } catch (BusinessException exception) {
            assertEquals(MSG12.CODE, exception.getCode());
        } catch (Throwable e) {
            fail("Expected BusinessException, but got: " + e.getClass().getName() + " : " + e.getMessage(), e);
        }
    }

    @Test
    void validateCustomer_ConnectionError_PermissiveFallback() {
        UUID customerId = UUID.randomUUID();
        when(restTemplate.getForEntity(anyString(), eq(Map.class), any(Map.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        assertDoesNotThrow(() -> userValidationAdapter.validateCustomer(customerId));
    }
}
