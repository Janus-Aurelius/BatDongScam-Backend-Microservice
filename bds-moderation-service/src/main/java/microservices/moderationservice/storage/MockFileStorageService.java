package microservices.moderationservice.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class MockFileStorageService implements FileStorageService {
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence";
        return "mock://" + folder + "/" + UUID.randomUUID() + "-" + fileName;
    }
}
