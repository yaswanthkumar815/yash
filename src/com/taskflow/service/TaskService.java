package com.taskflow.service;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.repository.TaskRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer providing core business logic for TaskFlow.
 */
public class TaskService {
    private final TaskRepository repository;

    public TaskService() {
        this(new TaskRepository());
    }

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    public List<Task> getAllTasks() {
        return repository.findAll();
    }

    public Optional<Task> getTaskById(int id) {
        return repository.findById(id);
    }

    public Task addTask(String title, String description, Priority priority) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty");
        }
        return repository.save(title.trim(), description, priority != null ? priority : Priority.MEDIUM);
    }

    public boolean toggleTask(int id) {
        Optional<Task> opt = repository.findById(id);
        if (opt.isPresent()) {
            Task current = opt.get();
            Task updated = current.withCompleted(!current.completed());
            return repository.update(updated);
        }
        return false;
    }

    public boolean deleteTask(int id) {
        return repository.deleteById(id);
    }

    public List<Task> searchTasks(String query, Priority priority, Boolean completed) {
        return repository.findAll().stream()
                .filter(t -> {
                    if (query != null && !query.trim().isEmpty()) {
                        String q = query.toLowerCase();
                        boolean matchTitle = t.title().toLowerCase().contains(q);
                        boolean matchDesc = t.description().toLowerCase().contains(q);
                        if (!matchTitle && !matchDesc) return false;
                    }
                    if (priority != null && t.priority() != priority) {
                        return false;
                    }
                    if (completed != null && t.completed() != completed) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStatistics() {
        List<Task> all = repository.findAll();
        long total = all.size();
        long completed = all.stream().filter(Task::completed).count();
        long pending = total - completed;
        long urgent = all.stream().filter(t -> !t.completed() && t.priority() == Priority.URGENT).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("pending", pending);
        stats.put("urgent", urgent);
        return stats;
    }
}
