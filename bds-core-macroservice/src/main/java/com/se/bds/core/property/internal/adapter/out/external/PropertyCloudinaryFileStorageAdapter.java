package com.se.bds.core.property.internal.adapter.out.external;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.se.bds.core.property.internal.application.port.out.PropertyFileStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class PropertyCloudinaryFileStorageAdapter implements PropertyFileStoragePort {

    private final Cloudinary cloudinary;

    public PropertyCloudinaryFileStorageAdapter(
            @Value("${cloudinary.cloud-name:default}") String cloudName,
            @Value("${cloudinary.api-key:default}") String apiKey,
            @Value("${cloudinary.api-secret:default}") String apiSecret) {

        if ("default".equals(cloudName)) {
            log.warn("Property Cloudinary configuration missing! Using fallback/mock implementation.");
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
        log.info("[PROPERTY EVENT] Uploading file to Cloudinary: folder={}, fileName={}", folder, fileName);
        if (cloudinary == null) {
            String fallbackUrl = "https://res.cloudinary.com/fallback-bds-platform/raw/upload/" + folder + "/" + fileName;
            log.info("[PROPERTY EVENT] Cloudinary fallback/mock triggered, returning: {}", fallbackUrl);
            return fallbackUrl;
        }
        try {
            Map params = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName,
                    "resource_type", "auto"
            );
            Map uploadResult = cloudinary.uploader().upload(fileBytes, params);
            String url = (String) uploadResult.get("secure_url");
            log.info("[PROPERTY EVENT] File uploaded successfully to Cloudinary: {}", url);
            return url;
        } catch (Exception e) {
            log.error("[PROPERTY EVENT] Cloudinary upload FAILED: fileName={}, error={}", fileName, e.getMessage());
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        log.info("[PROPERTY EVENT] Deleting file from Cloudinary: {}", fileUrl);
        if (cloudinary == null) {
            log.info("[PROPERTY EVENT] Cloudinary fallback/mock triggered for delete");
            return;
        }
        try {
            String publicId = extractPublicId(fileUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "auto"));
            log.info("[PROPERTY EVENT] File deleted successfully from Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.warn("[PROPERTY EVENT] Cloudinary delete FAILED for url={}: {}", fileUrl, e.getMessage());
        }
    }

    private String extractPublicId(String url) {
        if (url == null || !url.contains("/upload/")) return url;
        String afterUpload = url.split("/upload/")[1];
        if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
            return afterUpload.substring(afterUpload.indexOf("/") + 1);
        }
        return afterUpload;
    }
}
