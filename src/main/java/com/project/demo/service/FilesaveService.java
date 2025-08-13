package com.project.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.demo.logic.entity.history.HistoryEntry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilesaveService {

    private static final String FILE_PATH = "Filesave.json";
    private final ObjectMapper mapper;

    public FilesaveService() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule()); // Soporte para LocalDateTime
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Fechas legibles
    }

    public List<HistoryEntry> getUserHistory() {
        String email = getCurrentUserEmail();
        List<HistoryEntry> allHistory = readHistoryFile();
        return allHistory.stream()
                .filter(entry -> entry.getUserEmail().equals(email))
                .collect(Collectors.toList());
    }

    public HistoryEntry addHistory(String message, String fileName, String fileType, byte[] fileContent) throws IOException {
        String email = getCurrentUserEmail();
        List<HistoryEntry> history = readHistoryFile();
        File userDir = new File("uploads/" + email);
        if (!userDir.exists()) userDir.mkdirs();

        File outFile = new File(userDir, fileName);
        Files.write(outFile.toPath(), fileContent);

        HistoryEntry newEntry = new HistoryEntry(email, message, fileName, fileType, LocalDateTime.now());
        history.add(newEntry);
        saveHistoryFile(history);

        return newEntry;
    }


    private List<HistoryEntry> readHistoryFile() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }
            return mapper.readValue(file, new TypeReference<List<HistoryEntry>>() {});
        } catch (IOException e) {
            saveHistoryFile(new ArrayList<>());
            return new ArrayList<>();
        }
    }

    private void saveHistoryFile(List<HistoryEntry> history) {
        try {
            File tempFile = new File(FILE_PATH + ".tmp");
            mapper.writeValue(tempFile, history);
            Files.move(tempFile.toPath(), new File(FILE_PATH).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
