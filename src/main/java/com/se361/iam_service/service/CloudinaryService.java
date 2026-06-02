package com.se361.iam_service.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface CloudinaryService {
    String uploadFile(MultipartFile file, String folder) throws IOException;
    void deleteFile(String url) throws IOException;
}
