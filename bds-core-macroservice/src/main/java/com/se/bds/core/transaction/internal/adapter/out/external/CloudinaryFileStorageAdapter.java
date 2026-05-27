package com.se.bds.core.transaction.internal.adapter.out.external;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.se.bds.core.transaction.internal.application.port.out.FileStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class CloudinaryFileStorageAdapter implements FileStoragePort {

    private final Cloudinary cloudinary;

    public CloudinaryFileStorageAdapter(
            @Value("${cloudinary.cloud-name:default}") String cloudName,
            @Value("${cloudinary.api-key:default}") String apiKey,
            @Value("${cloudinary.api-secret:default}") String apiSecret) {
        
        // TODO: integrate with Cloudinary for PDF upload
        
        if ("default".equals(cloudName)) {
            log.warn("Cloudinary configuration missing! Using fallback/mock implementation.");
            this.cloudinary = null;
        } else {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
        }
    }

    @Override
    @org.springframework.retry.annotation.Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @org.springframework.retry.annotation.Backoff(delay = 500, multiplier = 2.0)
    )
    public String uploadFile(byte[] fileBytes, String folder, String fileName) {
        log.info("[EVENT] Uploading file to Cloudinary: folder={}, fileName={}", folder, fileName);
        if (cloudinary == null) {
            String fallbackUrl = "https://res.cloudinary.com/fallback-bds-platform/raw/upload/" + folder + "/" + fileName + ".pdf";
            log.info("[EVENT] Cloudinary fallback/mock triggered, returning: {}", fallbackUrl);
            return fallbackUrl;
        }
        try {
            Map params = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", fileName.replace(".pdf", ""),
                    "resource_type", "raw"
            );
            Map uploadResult = cloudinary.uploader().upload(fileBytes, params);
            String url = (String) uploadResult.get("secure_url");
            log.info("[EVENT] File uploaded successfully to Cloudinary: {}", url);
            return url;
        } catch (Exception e) {
            log.error("[EVENT] Cloudinary upload FAILED: fileName={}, error={}", fileName, e.getMessage());
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        log.info("[EVENT] Deleting file from Cloudinary: {}", fileUrl);
        if (cloudinary == null) {
            log.info("[EVENT] Cloudinary fallback/mock triggered for delete");
            return;
        }
        try {
            // Extract public ID from URL
            String publicId = extractPublicId(fileUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            log.info("[EVENT] File deleted successfully from Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.warn("[EVENT] Cloudinary delete FAILED for url={}: {}", fileUrl, e.getMessage());
        }
    }

    private String extractPublicId(String url) {
        // Simple extraction logic for demo/fallback
        if (url == null || !url.contains("/upload/")) return url;
        String afterUpload = url.split("/upload/")[1];
        // Skip version if present e.g. v12345678/folder/file
        if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
            return afterUpload.substring(afterUpload.indexOf("/") + 1);
        }
        return afterUpload;
    }
}
