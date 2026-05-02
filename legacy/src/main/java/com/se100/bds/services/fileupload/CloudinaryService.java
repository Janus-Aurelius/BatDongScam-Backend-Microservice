package com.se100.bds.services.fileupload;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface CloudinaryService {
    String uploadFile(MultipartFile file, String folder) throws IOException;
    void uploadMultipleFiles(MultipartFile[] files, String folder) throws IOException;
    void deleteFile(String url) throws IOException;
}