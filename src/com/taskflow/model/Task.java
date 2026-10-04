package com.taskflow.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Task model represented as an immutable Java 21 Record.
 */
public record Task(
    int id,
    String title,
    String description,
    Priority priority,
    boolean completed,
    String createdAt
) {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static Task create(int id, String title, String description, Priority priority) {
        String now = LocalDateTime.now().format(FORMATTER);
        return new Task(
            id,
            title != null ? title.trim() : "",
            description != null ? description.trim() : "",
            priority != null ? priority : Priority.MEDIUM,
            false,
            now
        );
    }

    public Task withCompleted(boolean isCompleted) {
        return new Task(id, title, description, priority, isCompleted, createdAt);
    }

    public Task withDetails(String newTitle, String newDescription, Priority newPriority) {
        return new Task(
            id,
            newTitle != null ? newTitle.trim() : this.title,
            newDescription != null ? newDescription.trim() : this.description,
            newPriority != null ? newPriority : this.priority,
            this.completed,
            this.createdAt
        );
    }
}
