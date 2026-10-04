package com.taskflow.model;

/**
 * Priority levels for tasks.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT;

    public static Priority fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MEDIUM;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
