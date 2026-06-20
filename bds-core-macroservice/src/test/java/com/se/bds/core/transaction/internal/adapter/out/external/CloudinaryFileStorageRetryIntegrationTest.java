package com.se.bds.core.transaction.internal.adapter.out.external;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.se.bds.core.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test suite verifying the Spring Retry (@Retryable) pattern on CloudinaryFileStorageAdapter.
 * Injects a mock Cloudinary SDK uploader client to simulate transient network glitches and counts retry attempts.
 */
public class CloudinaryFileStorageRetryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CloudinaryFileStorageAdapter fileStorageAdapter;

    private Cloudinary mockCloudinary;
    private Uploader mockUploader;

    @BeforeEach
    void setUp() {
        mockCloudinary = mock(Cloudinary.class);
        mockUploader = mock(Uploader.class);
        when(mockCloudinary.uploader()).thenReturn(mockUploader);

        // Inject the mock Cloudinary client into the Spring-managed bean using Reflection
        ReflectionTestUtils.setField(fileStorageAdapter, "cloudinary", mockCloudinary);
    }

    @Test
    void testCloudinaryUploadRetryWhenTransientFailuresOccur() throws Exception {
        byte[] fileBytes = "test-pdf-content".getBytes();
        String folder = "contracts";
        String fileName = "sample_contract.pdf";

        // Proof 1: Verify Spring Retry executes the upload up to 3 times when a mock connection error occurs
        // Note: maxAttempts = 3 is configured on the @Retryable annotation on the uploadFile method
        when(mockUploader.upload(any(byte[].class), any(Map.class)))
                .thenThrow(new IOException("Transient connection timeout on Cloudinary server"));

        try {
            fileStorageAdapter.uploadFile(fileBytes, folder, fileName);
            fail("Expected exception due to Cloudinary API failures");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Failed to upload file to Cloudinary"));
        }

        // Assert that the uploader was hit exactly 3 times (1 initial call + 2 retry attempts)
        verify(mockUploader, times(3)).upload(any(byte[].class), any(Map.class));
    }

    @Test
    void testCloudinaryUploadSucceedsAfterOneRetryAttempt() throws Exception {
        byte[] fileBytes = "test-pdf-content".getBytes();
        String folder = "contracts";
        String fileName = "sample_contract.pdf";

        Map<Object, Object> mockResponse = Map.of("secure_url", "https://res.cloudinary.com/success-url.pdf");

        // Proof 2: Verify that if the second attempt succeeds, the retry loop terminates and returns the successful URL
        when(mockUploader.upload(any(byte[].class), any(Map.class)))
                .thenThrow(new IOException("Transient connection error")) // First call fails
                .thenReturn(mockResponse); // Second call (first retry) succeeds

        String uploadedUrl = fileStorageAdapter.uploadFile(fileBytes, folder, fileName);

        // Assertions: verify URL matches and the client was hit exactly 2 times (1 initial call + 1 retry)
        assertEquals("https://res.cloudinary.com/success-url.pdf", uploadedUrl);
        verify(mockUploader, times(2)).upload(any(byte[].class), any(Map.class));
    }
}
