package com.se.bds.core.property.internal.application.port.out;

public interface PropertyFileStoragePort {
    String uploadFile(byte[] fileBytes, String folder, String fileName);
    void deleteFile(String fileUrl);
}
