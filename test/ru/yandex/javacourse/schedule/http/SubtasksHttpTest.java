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
import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SubtasksHttpTest {

    private TaskManager manager;
    private HttpTaskServer taskServer;
    private Gson gson;
    private HttpClient client;

    @BeforeEach
    public void setUp() throws IOException {
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

    private URI subtasksUri() {
        return URI.create("http://localhost:8080/subtasks");
    }

    @Test
    public void createSubtask_returns201_andSubtaskIsStored() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "For subtasks");
        manager.addNewEpic(epic);
        int epicId = epic.getId();

        Subtask subtask = new Subtask(
                "Sub 1",
                "Testing subtask create",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(15),
                LocalDateTime.now()
        );
        String json = gson.toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(subtasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());

        var subtasks = manager.getSubtasks();
        assertEquals(1, subtasks.size());
        assertEquals("Sub 1", subtasks.getFirst().getName());
        assertEquals("Testing subtask create", subtasks.getFirst().getDescription());
        assertEquals(epicId, subtasks.getFirst().getEpicId());
    }

    @Test
    public void createSubtask_withoutEpicId_returns404() throws IOException, InterruptedException {
        Subtask subtask = new Subtask(
                "Sub without epic",
                "No epicId",
                TaskStatus.NEW,
                0,
                Duration.ofMinutes(10),
                LocalDateTime.now()
        );
        String json = gson.toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(subtasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertTrue(manager.getSubtasks().isEmpty());
    }

    @Test
    public void createSubtask_withOverlappingTime_returns406_andDoesNotCreateSecondSubtask()
            throws IOException, InterruptedException {

        Epic epic = new Epic("Epic overlap", "Desc");
        manager.addNewEpic(epic);
        int epicId = epic.getId();

        Subtask s1 = new Subtask(
                "S1",
                "First",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(30),
                LocalDateTime.now()
        );
        String json1 = gson.toJson(s1);

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(subtasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .build();
        HttpResponse<String> resp1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp1.statusCode());

        Subtask s2 = new Subtask(
                "S2",
                "Overlapping",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(20),
                s1.getStartTime().plusMinutes(10)
        );
        String json2 = gson.toJson(s2);

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(subtasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();
        HttpResponse<String> resp2 = client.send(req2, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, resp2.statusCode());
        assertEquals(1, manager.getSubtasks().size());
    }

    @Test
    public void getSubtasks_returns200_andAllSubtasks() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Desc");
        manager.addNewEpic(epic);
        int epicId = epic.getId();

        Subtask s1 = new Subtask(
                "S1",
                "First",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(10),
                LocalDateTime.now()
        );
        Subtask s2 = new Subtask(
                "S2",
                "Second",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(15),
                LocalDateTime.now().plusMinutes(20)
        );
        manager.addNewSubtask(s1);
        manager.addNewSubtask(s2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(subtasksUri())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Subtask[] subtasks = gson.fromJson(response.body(), Subtask[].class);
        assertNotNull(subtasks);
        assertEquals(2, subtasks.length);
        assertEquals("S1", subtasks[0].getName());
        assertEquals("S2", subtasks[1].getName());
    }

    @Test
    public void getSubtaskById_notFound_returns404() throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:8080/subtasks/999");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    public void updateSubtask_returns201_andChangesAreSaved() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Desc");
        manager.addNewEpic(epic);
        int epicId = epic.getId();

        Subtask s = new Subtask(
                "Old sub",
                "Old desc",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(10),
                LocalDateTime.now()
        );
        manager.addNewSubtask(s);
        int id = s.getId();

        s.setName("Updated sub");
        s.setDescription("Updated desc");
        s.setStatus(TaskStatus.DONE);

        String json = gson.toJson(s);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(subtasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        Subtask updated = manager.getSubtask(id);
        assertEquals("Updated sub", updated.getName());
        assertEquals("Updated desc", updated.getDescription());
        assertEquals(TaskStatus.DONE, updated.getStatus());
    }

    @Test
    public void updateSubtask_notFound_returns404() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Desc");
        manager.addNewEpic(epic);
        int epicId = epic.getId();

        Subtask s = new Subtask(
                "Ghost sub",
                "No such sub",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(10),
                LocalDateTime.now()
        );
        s.setId(999);
        String json = gson.toJson(s);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(subtasksUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    public void deleteSubtaskById_returns200_andSubtaskRemoved() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Desc");
        manager.addNewEpic(epic);
        int epicId = epic.getId();

        Subtask s1 = new Subtask(
                "S1",
                "First",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(10),
                LocalDateTime.now()
        );
        Subtask s2 = new Subtask(
                "S2",
                "Second",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(15),
                LocalDateTime.now().plusMinutes(20)
        );
        manager.addNewSubtask(s1);
        manager.addNewSubtask(s2);

        int idToDelete = s1.getId();
        URI uri = URI.create("http://localhost:8080/subtasks/" + idToDelete);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        var subtasks = manager.getSubtasks();
        assertEquals(1, subtasks.size());
        assertEquals(s2.getId(), subtasks.getFirst().getId());
    }

    @Test
    public void deleteAllSubtasks_returns200_andAllSubtasksRemoved() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Desc");
        manager.addNewEpic(epic);
        int epicId = epic.getId();

        Subtask s1 = new Subtask(
                "S1",
                "First",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(10),
                LocalDateTime.now()
        );
        Subtask s2 = new Subtask(
                "S2",
                "Second",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(15),
                LocalDateTime.now().plusMinutes(20)
        );
        manager.addNewSubtask(s1);
        manager.addNewSubtask(s2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(subtasksUri())
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(manager.getSubtasks().isEmpty());
    }
}
