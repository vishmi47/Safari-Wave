package com.example.safariwave_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class PromotionImageStorageService {

    private static final Path UPLOAD_DIRECTORY =
            Paths.get("uploads", "promotions").toAbsolutePath().normalize();

    public String store(MultipartFile file) {
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null
                ? "banner"
                : file.getOriginalFilename());
        String extension = "";
        int extensionIndex = originalName.lastIndexOf('.');

        if (extensionIndex >= 0) {
            extension = originalName.substring(extensionIndex).toLowerCase();
        }

        if (!extension.matches("\\.(jpg|jpeg|png|webp)")) {
            throw new IllegalArgumentException("Banner must be JPG, PNG, or WEBP.");
        }

        try {
            Files.createDirectories(UPLOAD_DIRECTORY);
            String filename = UUID.randomUUID() + extension;
            Path destination = UPLOAD_DIRECTORY.resolve(filename).normalize();

            if (!destination.getParent().equals(UPLOAD_DIRECTORY)) {
                throw new IllegalArgumentException("Invalid banner filename.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/promotions/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store promotion banner.", exception);
        }
    }
}