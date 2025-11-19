package ru.yandex.javacourse.schedule.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.http.adapter.DurationAdapter;
import ru.yandex.javacourse.schedule.http.adapter.LocalDateTimeAdapter;
import ru.yandex.javacourse.schedule.manager.InMemoryTaskManager;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TasksHttpTest {

    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        manager = new InMemoryTaskManager();
        taskServer = new HttpTaskServer(manager);
        gson = new GsonBuilder()
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        client = HttpClient.newHttpClient();

        taskServer.start();
    }

    @AfterEach
    public void shutDown() {
        taskServer.stop();
    }

    private URI tasksUri() {
        return URI.create("http://localhost:8080/tasks");
    }

    @Test
    void createTask_returns201_andTaskIsStored() throws IOException, InterruptedException {
        Task task = new Task(
                "Test task",
                "Testing create",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now()
        );
        String json = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(tasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Ожидался код 201 при создании задачи");

        List<Task> tasks = manager.getTasks();
        assertEquals(1, tasks.size(), "После создания должна быть 1 задача");
        assertEquals("Test task", tasks.getFirst().getName());
    }

    @Test
    void createTask_withOverlappingTime_returns406_andDoesNotCreateSecondTask()
            throws IOException, InterruptedException {

        Task t1 = new Task(
                "T1",
                "First",
                TaskStatus.NEW,
                Duration.ofMinutes(10),
                LocalDateTime.now()
        );
        String json1 = gson.toJson(t1);
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(tasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .build();
        HttpResponse<String> resp1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp1.statusCode());

        Task t2 = new Task(
                "T2",
                "Overlapping",
                TaskStatus.NEW,
                Duration.ofMinutes(10),
                t1.getStartTime().plusMinutes(5)
        );
        String json2 = gson.toJson(t2);
        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(tasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();
        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, resp2.statusCode(), "Ожидался код 406 при пересечении задач по времени");
        assertEquals(1, manager.getTasks().size(), "В менеджере должна остаться только первая задача");
    }

    @Test
    void getTasks_returns200_andAllTasks() throws IOException, InterruptedException {
        Task t1 = new Task(
                "T1",
                "Task 1",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now()
        );
        Task t2 = new Task(
                "T2",
                "Task 2",
                TaskStatus.NEW,
                Duration.ofMinutes(10),
                LocalDateTime.now().plusMinutes(10)
        );
        manager.addNewTask(t1);
        manager.addNewTask(t2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(tasksUri())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Task[] tasksFromServer = gson.fromJson(response.body(), Task[].class);

        assertNotNull(tasksFromServer, "С сервера вернулся null вместо списка задач");
        assertEquals(2, tasksFromServer.length, "Некорректное количество задач из API");
        assertEquals("T1", tasksFromServer[0].getName());
        assertEquals("T2", tasksFromServer[1].getName());
    }

    @Test
    void getTaskById_returns200_andCorrectTask() throws IOException, InterruptedException {
        Task t = new Task(
                "Single",
                "By id",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now()
        );
        manager.addNewTask(t);
        int id = t.getId();

        URI uri = URI.create("http://localhost:8080/tasks/" + id);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Task fromServer = gson.fromJson(response.body(), Task.class);
        assertEquals(id, fromServer.getId());
        assertEquals("Single", fromServer.getName());
    }

    @Test
    void getTaskById_notFound_returns404() throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:8080/tasks/999");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Для несуществующей задачи должен быть 404");
    }

    @Test
    void updateTask_returns201_andChangesAreSaved() throws IOException, InterruptedException {
        Task t = new Task(
                "Old",
                "Old desc",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now()
        );
        manager.addNewTask(t);
        int id = t.getId();

        t.setName("Updated");
        t.setDescription("Updated desc");
        t.setStatus(TaskStatus.IN_PROGRESS);

        String json = gson.toJson(t);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(tasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        Task updated = manager.getTask(id);
        assertEquals("Updated", updated.getName());
        assertEquals("Updated desc", updated.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void updateTask_notFound_returns404() throws IOException, InterruptedException {
        Task t = new Task(
                "Ghost",
                "No such task",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now()
        );
        t.setId(999);
        String json = gson.toJson(t);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(tasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Обновление несуществующей задачи должно вернуть 404");
    }

    @Test
    void deleteTaskById_returns200_andTaskRemoved() throws IOException, InterruptedException {
        Task t1 = new Task(
                "T1",
                "Task 1",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now()
        );
        Task t2 = new Task(
                "T2",
                "Task 2",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now().plusMinutes(10)
        );
        manager.addNewTask(t1);
        manager.addNewTask(t2);

        int idToDelete = t1.getId();
        URI uri = URI.create("http://localhost:8080/tasks/" + idToDelete);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(1, manager.getTasks().size(), "Должна остаться одна задача");
        assertEquals(t2.getId(), manager.getTasks().getFirst().getId());
    }

    @Test
    void deleteAllTasks_returns200_andAllTasksRemoved() throws IOException, InterruptedException {
        Task t1 = new Task(
                "T1",
                "Task 1",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now()
        );
        Task t2 = new Task(
                "T2",
                "Task 2",
                TaskStatus.NEW,
                Duration.ofMinutes(5),
                LocalDateTime.now().plusMinutes(10)
        );
        manager.addNewTask(t1);
        manager.addNewTask(t2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(tasksUri())
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(manager.getTasks().isEmpty(), "После DELETE /tasks список задач должен быть пустым");
    }
}
