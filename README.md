# ⚡ TaskFlow

> **A modern, lightweight Task & Productivity Management Hub built in pure Java 21** with an interactive CLI, embedded HTTP REST API, and a sleek responsive Web Dashboard. Zero external runtime dependencies required!

---

## 🌟 Highlights & Java 21 Features

- **Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`)**: Powers the embedded HTTP server for high concurrency and ultra-low memory overhead.
- **Records (`record Task(...)`)**: Concise, immutable data model with built-in validation and copy-with semantics.
- **Pattern Matching & Switch Expressions**: Clean, type-safe control flow for priority mapping and CLI command dispatching.
- **Zero-Dependency Architecture**: Runs directly on any standard Java 21 JDK without needing Maven, Gradle, or third-party libraries.
- **Embedded Web Dashboard**: Modern, responsive single-page web UI served directly at `http://localhost:8080`.
- **Interactive Terminal CLI**: Formatted ASCII table, color-coded priority badges, and quick keyboard navigation.
- **File Persistence**: Automatic JSON storage in `data/tasks.json` with thread-safe read/write locking.
- **Built-in Automated Test Suite**: Built-in unit and integration tests (`--test`).

---

## 📂 Project Architecture

```
task-flow/
├── src/
│   └── com/
│       └── taskflow/
│           ├── Main.java                 # Entry point (CLI & server lifecycle)
│           ├── model/
│           │   ├── Priority.java         # Priority enum (LOW, MEDIUM, HIGH, URGENT)
│           │   └── Task.java             # Immutable Java 21 record
│           ├── repository/
│           │   └── TaskRepository.java   # Thread-safe JSON file persistence
│           ├── service/
│           │   └── TaskService.java      # Core business logic & analytics
│           ├── server/
│           │   └── TaskServer.java       # HTTP REST Server using Virtual Threads
│           ├── cli/
│           │   └── TaskCli.java          # Interactive console interface
│           ├── util/
│           │   └── JsonUtil.java         # Zero-dependency JSON parser & serializer
│           └── test/
│               └── TaskServiceTest.java  # Built-in automated test suite
├── static/
│   └── index.html                        # Modern single-page web dashboard
├── data/
│   └── tasks.json                        # Auto-created persistent storage
├── build.bat                             # Windows compilation & packaging script
├── run.bat                               # Application launcher script
└── README.md
```

---

## 🚀 Quick Start

### 1. Build the Project
Open a terminal in the `task-flow` directory and run:

```bat
build.bat
```

Or manually compile and package:
```bash
mkdir bin
javac -encoding UTF-8 -d bin src/com/taskflow/model/*.java src/com/taskflow/util/*.java src/com/taskflow/repository/*.java src/com/taskflow/service/*.java src/com/taskflow/server/*.java src/com/taskflow/cli/*.java src/com/taskflow/test/*.java src/com/taskflow/Main.java
jar cvfe taskflow.jar com.taskflow.Main -C bin com
```

### 2. Run the Application

#### Default Mode (Interactive CLI + Web Server)
```bat
run.bat
```
or
```bash
java -jar taskflow.jar
```
*Starts the HTTP server at `http://localhost:8080` in the background and opens the interactive terminal menu.*

#### Headless Server Mode (Web & API Only)
```bash
java -jar taskflow.jar --server-only
```
Or specify a custom port:
```bash
java -jar taskflow.jar --server-only --port 9090
```

#### Run Automated Tests
```bash
java -jar taskflow.jar --test
```

---

## 🌐 Web Dashboard

Once started, open your browser to:
👉 **[http://localhost:8080](http://localhost:8080)**

Features in the Web UI:
- **Productivity Metrics**: Real-time counter for Total, Pending, Completed, and Urgent tasks.
- **Search & Filtering**: Instant search across titles and descriptions, filter by priority and completion tabs.
- **Add / Edit / Delete**: Quick modal for creating tasks, one-click toggle, and deletion.
- **Responsive Design**: Works seamlessly on mobile and desktop screens.

---

## 📡 REST API Reference

The embedded HTTP server exposes a full RESTful JSON API:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/tasks` | Get all tasks (supports `?search=...&priority=...&completed=...`) |
| `POST` | `/api/tasks` | Create a task (`{"title": "...", "description": "...", "priority": "..."}`) |
| `POST` | `/api/tasks/toggle` | Toggle completion status (`{"id": 1}`) |
| `POST` | `/api/tasks/delete` | Delete a task (`{"id": 1}`) |
| `GET` | `/api/stats` | Get productivity statistics |

### Example cURL Requests

**Fetch all tasks:**
```bash
curl http://localhost:8080/api/tasks
```

**Create a new task:**
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Implement OAuth","description":"Add user login","priority":"HIGH"}'
```

**Toggle task status:**
```bash
curl -X POST http://localhost:8080/api/tasks/toggle \
  -H "Content-Type: application/json" \
  -d '{"id": 1}'
```
