package helpdesk.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service
public class StorageService {
	// Define the root location for storing uploaded files
    private final Path rootLocation = Paths.get("upload-dir");

    public StorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }
            // Generate a unique filename to avoid conflicts
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Save the file
            Path destinationFile = this.rootLocation.resolve(Paths.get(uniqueFilename))
                                     .normalize().toAbsolutePath();
            Files.copy(file.getInputStream(), destinationFile);

            // Return the path to be saved in the database
            return "/uploads/" + uniqueFilename; 
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }
}