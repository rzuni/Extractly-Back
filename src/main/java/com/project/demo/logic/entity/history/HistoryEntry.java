package com.project.demo.logic.entity.history;

import java.time.LocalDateTime;

public class HistoryEntry {
    private String userEmail;
    private String message;
    private String fileName;
    private String fileType;
    private LocalDateTime timestamp;

    public HistoryEntry() {}

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public HistoryEntry(String userEmail, String message, String fileName, String fileType, LocalDateTime timestamp) {
        this.userEmail = userEmail;
        this.message = message;
        this.fileName = fileName;
        this.fileType = fileType;
        this.timestamp = timestamp;
    }
}
