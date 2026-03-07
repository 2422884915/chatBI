package com.chatbi.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class WeeklyReportTemplateService {

    private static final Path TEMPLATE_PATH = Paths.get("data", "weekly-report-template.md");
    private volatile String template;

    public WeeklyReportTemplateService() {
        this.template = loadFromDisk();
    }

    public synchronized String saveTemplate(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Template content cannot be empty.");
        }
        writeToDisk(content);
        this.template = content;
        return content;
    }

    public synchronized String saveTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Template file cannot be empty.");
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return saveTemplate(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template file: " + e.getMessage(), e);
        }
    }

    public Optional<String> getTemplate() {
        return Optional.ofNullable(template).filter(StringUtils::hasText);
    }

    private String loadFromDisk() {
        try {
            if (Files.exists(TEMPLATE_PATH)) {
                String content = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);
                if (StringUtils.hasText(content)) {
                    return content;
                }
            }
        } catch (IOException ignored) {
            // Keep startup resilient. If reading fails, template stays null.
        }
        return null;
    }

    private void writeToDisk(String content) {
        try {
            Path parent = TEMPLATE_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(TEMPLATE_PATH, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist template: " + e.getMessage(), e);
        }
    }
}
