package com.boat.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Request {
    private Long id;
    private Long userChatId;
    private String description;
    private LocalDateTime createdAt;
    private String status;
    private Long captainChatId;

    public Request() {
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserChatId() {
        return userChatId;
    }

    public void setUserChatId(Long userChatId) {
        this.userChatId = userChatId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCaptainChatId() {
        return captainChatId;
    }

    public void setCaptainChatId(Long captainChatId) {
        this.captainChatId = captainChatId;
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) return "неизвестно";
        return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}