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

import static org.junit.jupiter.api.Assertions.*;

class HistoryHttpTest {

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

    private URI historyUri() {
        return URI.create("http://localhost:8080/history");
    }

    @Test
    public void getHistory_whenEmpty_returns200AndEmptyArray() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(historyUri())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Task[] history = gson.fromJson(response.body(), Task[].class);
        assertNotNull(history);
        assertEquals(0, history.length);
    }

    @Test
    public void getHistory_returnsEntitiesInAccessOrder() throws IOException, InterruptedException {
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
                LocalDateTime.now().plusMinutes(15)
        );
        manager.addNewTask(t1);
        manager.addNewTask(t2);

        int id1 = t1.getId();
        int id2 = t2.getId();

        manager.getTask(id1);
        manager.getTask(id2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(historyUri())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Task[] history = gson.fromJson(response.body(), Task[].class);
        assertNotNull(history);
        assertEquals(2, history.length);
        assertEquals("T1", history[0].getName());
        assertEquals("T2", history[1].getName());
    }

    @Test
    public void history_nonGetMethod_returns500() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(historyUri())
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(500, response.statusCode());
    }
}
