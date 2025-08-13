package com.project.demo.rest.filesave;

import com.project.demo.logic.entity.history.HistoryEntry;
import com.project.demo.service.FilesaveService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class FilesaveController {

    private final FilesaveService filesaveService;

    public FilesaveController(FilesaveService filesaveService) {
        this.filesaveService = filesaveService;
    }

    @GetMapping
    public ResponseEntity<List<HistoryEntry>> getHistory() {
        return ResponseEntity.ok(filesaveService.getUserHistory());
    }

    @PostMapping
    public ResponseEntity<HistoryEntry> addHistory(@RequestBody Map<String, String> payload) {
        try {
            String message = payload.get("message");
            String fileName = payload.get("fileName");
            String fileType = payload.get("fileType");
            String base64Content = payload.get("fileContent");

            byte[] fileContent = Base64.getDecoder().decode(base64Content);

            HistoryEntry createdEntry = filesaveService.addHistory(message, fileName, fileType, fileContent);
            return ResponseEntity.ok(createdEntry);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        File file = new File("uploads/" + email + "/" + fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);

        String contentType = "application/octet-stream";
        if (fileName.endsWith(".pdf")) contentType = "application/pdf";
        else if (fileName.endsWith(".pptx")) contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
