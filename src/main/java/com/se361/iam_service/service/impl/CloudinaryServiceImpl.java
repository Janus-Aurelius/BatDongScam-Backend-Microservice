package com.se361.iam_service.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.se361.iam_service.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            return null; // IAM: trả null thay vì throw — caller tự quyết định
        }
        return cloudinary.uploader()
                .upload(file.getBytes(), ObjectUtils.asMap("folder", folder))
                .get("secure_url")
                .toString();
    }

    @Override
    public void deleteFile(String url) throws IOException {
        if (url == null || url.isBlank()) return;

        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            log.warn("Invalid Cloudinary URL, skipping delete: {}", url);
            return;
        }

        String afterUpload = url.substring(uploadIndex + 8);

        // Bỏ version prefix (vXXXXXX/)
        if (afterUpload.startsWith("v")) {
            int slash = afterUpload.indexOf("/");
            if (slash != -1) afterUpload = afterUpload.substring(slash + 1);
        }

        // Bỏ extension
        int dot = afterUpload.lastIndexOf(".");
        if (dot != -1) afterUpload = afterUpload.substring(0, dot);

        cloudinary.uploader().destroy(afterUpload, ObjectUtils.emptyMap());
    }
}
