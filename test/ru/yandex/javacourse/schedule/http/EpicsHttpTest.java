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

class EpicsHttpTest {

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

    private URI epicsUri() {
        return URI.create("http://localhost:8080/epics");
    }

    @Test
    void createEpic_returns201_andEpicIsStored() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Testing epic create");
        String json = gson.toJson(epic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(epicsUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Ожидался код 201 при создании эпика");

        var epics = manager.getEpics();
        assertEquals(1, epics.size(), "После создания должен быть 1 эпик");
        assertEquals("Epic 1", epics.getFirst().getName());
        assertEquals("Testing epic create", epics.getFirst().getDescription());
    }

    @Test
    void getEpicById_notFound_returns404() throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:8080/epics/999");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode(), "Для несуществующего эпика должен быть 404");
    }

    @Test
    void getEpicSubtasks_returns200_andAllSubtasksOfEpic() throws IOException, InterruptedException {

        Epic epic = new Epic("Epic with subtasks", "Desc");
        manager.addNewEpic(epic);
        int epicId = epic.getId();

        Subtask s1 = new Subtask(
                "Sub 1",
                "First",
                TaskStatus.NEW,
                epicId,
                Duration.ofMinutes(15),
                LocalDateTime.now()
        );
        Subtask s2 = new Subtask(
                "Sub 2",
                "Second",
                TaskStatus.IN_PROGRESS,
                epicId,
                Duration.ofMinutes(30),
                LocalDateTime.now().plusMinutes(20)
        );
        manager.addNewSubtask(s1);
        manager.addNewSubtask(s2);

        URI uri = URI.create("http://localhost:8080/epics/" + epicId + "/subtasks");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Subtask[] subtasks = gson.fromJson(response.body(), Subtask[].class);
        assertNotNull(subtasks, "С сервера вернулся null вместо списка подзадач эпика");
        assertEquals(2, subtasks.length, "Неверное количество подзадач эпика из API");
        assertEquals("Sub 1", subtasks[0].getName());
        assertEquals("Sub 2", subtasks[1].getName());
    }

    @Test
    void getEpicSubtasks_forNonExistingEpic_returns404() throws IOException, InterruptedException {
        URI uri = URI.create("http://localhost:8080/epics/999/subtasks");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode(), "Для несуществующего эпика при запросе подзадач ожидается 404");
    }

    @Test
    void updateEpic_returns201_andChangesAreSaved() throws IOException, InterruptedException {
        Epic epic = new Epic("Old epic", "Old desc");
        manager.addNewEpic(epic);
        int id = epic.getId();

        epic.setName("Updated epic");
        epic.setDescription("Updated desc");
        String json = gson.toJson(epic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(epicsUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        Epic updated = manager.getEpic(id);
        assertEquals("Updated epic", updated.getName());
        assertEquals("Updated desc", updated.getDescription());
    }

    @Test
    void updateEpic_notFound_returns404() throws IOException, InterruptedException {
        Epic epic = new Epic("Ghost", "No such epic");
        epic.setId(999);
        String json = gson.toJson(epic);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(epicsUri())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode(), "Обновление несуществующего эпика должно вернуть 404");
    }

    @Test
    void deleteEpicById_returns201_andEpicRemoved() throws IOException, InterruptedException {
        Epic e1 = new Epic("Epic 1", "Desc 1");
        Epic e2 = new Epic("Epic 2", "Desc 2");
        manager.addNewEpic(e1);
        manager.addNewEpic(e2);

        int idToDelete = e1.getId();
        URI uri = URI.create("http://localhost:8080/epics/" + idToDelete);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        var epics = manager.getEpics();
        assertEquals(1, epics.size(), "Должен остаться один эпик");
        assertEquals(e2.getId(), epics.getFirst().getId());
    }

    @Test
    void deleteAllEpics_returns201_andAllEpicsRemoved() throws IOException, InterruptedException {
        Epic e1 = new Epic("Epic 1", "Desc 1");
        Epic e2 = new Epic("Epic 2", "Desc 2");
        manager.addNewEpic(e1);
        manager.addNewEpic(e2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(epicsUri())
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        assertTrue(manager.getEpics().isEmpty(), "После DELETE /epics список эпиков должен быть пустым");
    }
}
