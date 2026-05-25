package com.se.bds.core.transaction.internal.application.port.out;

/**
 * Outbound port for file storage operations (US-008).
 * Abstracts Cloudinary to allow swapping storage providers.
 */
public interface FileStoragePort {

    /**
     * Uploads a file (e.g., generated PDF) to cloud storage.
     *
     * @param fileBytes the file content as byte array
     * @param folder    the storage folder path
     * @param fileName  the desired file name
     * @return the public URL of the uploaded file
     */
    String uploadFile(byte[] fileBytes, String folder, String fileName);

    /**
     * Deletes a file from cloud storage.
     *
     * @param fileUrl the public URL of the file to delete
     */
    void deleteFile(String fileUrl);
}
