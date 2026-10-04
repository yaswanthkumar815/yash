package com.taskflow.util;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lightweight, zero-dependency JSON utility for TaskFlow.
 */
public class JsonUtil {

    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\f", "\f");
    }

    public static String taskToJson(Task t) {
        if (t == null) return "null";
        return String.format(
            "{\"id\":%d,\"title\":\"%s\",\"description\":\"%s\",\"priority\":\"%s\",\"completed\":%b,\"createdAt\":\"%s\"}",
            t.id(),
            escape(t.title()),
            escape(t.description()),
            t.priority().name(),
            t.completed(),
            escape(t.createdAt())
        );
    }

    public static String taskListToJson(List<Task> tasks) {
        if (tasks == null) return "[]";
        return "[\n  " + tasks.stream().map(JsonUtil::taskToJson).collect(Collectors.joining(",\n  ")) + "\n]";
    }

    public static String mapToJson(Map<String, ?> map) {
        if (map == null) return "{}";
        return "{" + map.entrySet().stream()
                .map(e -> {
                    String key = "\"" + escape(e.getKey()) + "\"";
                    Object val = e.getValue();
                    String valStr;
                    if (val == null) {
                        valStr = "null";
                    } else if (val instanceof Number || val instanceof Boolean) {
                        valStr = val.toString();
                    } else {
                        valStr = "\"" + escape(val.toString()) + "\"";
                    }
                    return key + ":" + valStr;
                })
                .collect(Collectors.joining(",")) + "}";
    }

    public static String extractString(String json, String key) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescape(matcher.group(1));
        }
        return null;
    }

    public static Integer extractInt(String json, String key) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public static Boolean extractBoolean(String json, String key) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return null;
    }

    public static Task parseTask(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        Integer id = extractInt(json, "id");
        String title = extractString(json, "title");
        String description = extractString(json, "description");
        String priorityStr = extractString(json, "priority");
        Boolean completed = extractBoolean(json, "completed");
        String createdAt = extractString(json, "createdAt");

        return new Task(
            id != null ? id : 0,
            title != null ? title : "",
            description != null ? description : "",
            Priority.fromString(priorityStr),
            completed != null && completed,
            createdAt != null ? createdAt : ""
        );
    }

    public static List<Task> parseTaskList(String json) {
        List<Task> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) {
            return list;
        }

        // Match individual object chunks safely
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        String objJson = json.substring(start, i + 1);
                        Task task = parseTask(objJson);
                        if (task != null) {
                            list.add(task);
                        }
                        start = -1;
                    }
                }
            }
        }

        return list;
    }
}
