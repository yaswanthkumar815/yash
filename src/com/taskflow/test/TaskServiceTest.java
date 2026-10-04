package com.taskflow.test;

import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.repository.TaskRepository;
import com.taskflow.service.TaskService;
import com.taskflow.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Built-in self-contained automated test suite for TaskFlow (zero external testing dependencies).
 */
public class TaskServiceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void runAllTests() {
        System.out.println("======================================================================");
        System.out.println(" 🧪  Running TaskFlow Automated Test Suite");
        System.out.println("======================================================================");

        testTaskCreation();
        testToggleCompletion();
        testDeleteTask();
        testSearchAndFilter();
        testJsonSerializationAndParsing();
        testStatisticsCalculation();

        System.out.println("\n----------------------------------------------------------------------");
        System.out.println("  Tests Passed: " + passed + " | Tests Failed: " + failed);
        if (failed == 0) {
            System.out.println("  🎉 ALL TESTS PASSED SUCCESSFULLY!");
        } else {
            System.err.println("  ⚠️ SOME TESTS FAILED!");
        }
        System.out.println("======================================================================");
    }

    private static void assertEquals(Object expected, Object actual, String testName) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            System.out.println("  ✅ PASS: " + testName);
            passed++;
        } else {
            System.err.println("  ❌ FAIL: " + testName + " - Expected: " + expected + ", Actual: " + actual);
            failed++;
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            System.out.println("  ✅ PASS: " + testName);
            passed++;
        } else {
            System.err.println("  ❌ FAIL: " + testName + " (expected true, got false)");
            failed++;
        }
    }

    private static TaskService createTempService() {
        try {
            Path tempDir = Files.createTempDirectory("taskflow-test-");
            Path testFile = tempDir.resolve("test-tasks.json");
            TaskRepository repo = new TaskRepository(testFile);
            return new TaskService(repo);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary test repository", e);
        }
    }

    private static void testTaskCreation() {
        TaskService service = createTempService();
        int initialSize = service.getAllTasks().size();

        Task task = service.addTask("Test Unit Feature", "Detailed description", Priority.HIGH);
        assertEquals("Test Unit Feature", task.title(), "testTaskCreation.title");
        assertEquals("Detailed description", task.description(), "testTaskCreation.description");
        assertEquals(Priority.HIGH, task.priority(), "testTaskCreation.priority");
        assertEquals(false, task.completed(), "testTaskCreation.completedDefault");
        assertEquals(initialSize + 1, service.getAllTasks().size(), "testTaskCreation.countIncreased");
    }

    private static void testToggleCompletion() {
        TaskService service = createTempService();
        Task task = service.addTask("Toggle Test", "Check toggling", Priority.MEDIUM);
        int id = task.id();

        boolean toggled = service.toggleTask(id);
        assertTrue(toggled, "testToggleCompletion.firstToggleSuccess");
        assertTrue(service.getTaskById(id).orElseThrow().completed(), "testToggleCompletion.isCompleted");

        service.toggleTask(id);
        assertTrue(!service.getTaskById(id).orElseThrow().completed(), "testToggleCompletion.isPendingAgain");
    }

    private static void testDeleteTask() {
        TaskService service = createTempService();
        Task task = service.addTask("Delete Me", "Temporary item", Priority.LOW);
        int id = task.id();

        boolean deleted = service.deleteTask(id);
        assertTrue(deleted, "testDeleteTask.deleteSuccess");
        assertTrue(service.getTaskById(id).isEmpty(), "testDeleteTask.taskNotFoundAfterDelete");
    }

    private static void testSearchAndFilter() {
        TaskService service = createTempService();
        service.addTask("Write Documentation", "Doc for Java 21", Priority.HIGH);
        service.addTask("Fix Bug in Server", "Investigate connection timeout", Priority.URGENT);

        List<Task> searchResult = service.searchTasks("documentation", null, null);
        assertEquals(1, searchResult.size(), "testSearchAndFilter.keywordMatch");

        List<Task> urgentTasks = service.searchTasks(null, Priority.URGENT, null);
        assertTrue(urgentTasks.stream().anyMatch(t -> t.title().contains("Fix Bug")), "testSearchAndFilter.priorityFilter");
    }

    private static void testJsonSerializationAndParsing() {
        Task task = Task.create(99, "JSON \"Test\" & Special <Chars>", "Line 1\nLine 2", Priority.URGENT);
        String json = JsonUtil.taskToJson(task);
        Task parsed = JsonUtil.parseTask(json);

        assertEquals(task.id(), parsed.id(), "testJson.id");
        assertEquals(task.title(), parsed.title(), "testJson.title");
        assertEquals(task.description(), parsed.description(), "testJson.description");
        assertEquals(task.priority(), parsed.priority(), "testJson.priority");
        assertEquals(task.completed(), parsed.completed(), "testJson.completed");
    }

    private static void testStatisticsCalculation() {
        TaskService service = createTempService();
        service.addTask("Urgent 1", "", Priority.URGENT);
        Task toComplete = service.addTask("Normal 1", "", Priority.MEDIUM);
        service.toggleTask(toComplete.id());

        Map<String, Object> stats = service.getStatistics();
        assertTrue((long) stats.get("total") >= 2, "testStatistics.total");
        assertTrue((long) stats.get("completed") >= 1, "testStatistics.completed");
        assertTrue((long) stats.get("urgent") >= 1, "testStatistics.urgent");
    }
}
