package ru.yandex.javacourse.schedule.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ru.yandex.javacourse.schedule.http.adapter.DurationAdapter;
import ru.yandex.javacourse.schedule.http.adapter.LocalDateTimeAdapter;
import ru.yandex.javacourse.schedule.http.handler.BaseHttpHandler;
import ru.yandex.javacourse.schedule.manager.ManagerTimeIntersectionException;
import ru.yandex.javacourse.schedule.manager.Managers;
import ru.yandex.javacourse.schedule.manager.NotFoundException;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class HttpTaskServer {

    private static final URI BASE_URI = URI.create("http://localhost:8080");
    private static final int PORT = BASE_URI.getPort();

    private final HttpServer httpServer;
    private final TaskManager manager;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Duration.class, new DurationAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public HttpTaskServer(TaskManager manager) throws IOException {
        this.manager = manager;
        this.httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);

        httpServer.createContext("/tasks", new TasksHandler());
        httpServer.createContext("/subtasks", new SubtasksHandler());
        httpServer.createContext("/epics", new EpicsHandler());
        httpServer.createContext("/history", new HistoryHandler());
        httpServer.createContext("/prioritized", new PrioritizedHandler());
    }

    public HttpTaskServer() throws IOException {
        this(Managers.getDefault());
    }

    public void start() {
        httpServer.start();
        System.out.println("HTTP-сервер запущен на " + PORT + " порту!");
    }

    public void stop() {
        httpServer.stop(0);
        System.out.println("HTTP-сервер остановлен");
    }

    public static void main(String[] args) throws IOException {
        HttpTaskServer server = new HttpTaskServer();
        server.start();
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private abstract class EntityHandler<T extends Task> extends BaseHttpHandler implements HttpHandler {

        protected abstract Class<T> getEntityClass();

        protected abstract List<T> getAll();

        protected abstract T getById(int id);

        protected abstract void create(T entity);

        protected abstract void update(T entity);

        protected abstract void deleteAll();

        protected abstract void deleteById(int id);

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            try {
                switch (method) {
                    case "GET":
                        handleGet(exchange);
                        break;
                    case "POST":
                        handlePost(exchange);
                        break;
                    case "DELETE":
                        handleDelete(exchange);
                        break;
                    default:
                        sendServerError(exchange);
                }
            } catch (ManagerTimeIntersectionException | IllegalArgumentException e) {
                sendHasInteractions(exchange);
            } catch (NotFoundException e) {
                sendNotFound(exchange, e.getMessage());
            } catch (Exception e) {
                sendServerError(exchange);
            }
        }

        private Integer extractId(HttpExchange exchange) {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length > 2) {
                try {
                    return Integer.parseInt(parts[2]);
                } catch (NumberFormatException ignored) {
                }
            }
            return null;
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            Integer id = extractId(exchange);
            if (id == null) {
                sendText(exchange, gson.toJson(getAll()));
            } else {
                sendText(exchange, gson.toJson(getById(id)));
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            T entity = gson.fromJson(readBody(exchange), getEntityClass());
            Integer idFromPath = extractId(exchange);
            if (idFromPath != null) {
                entity.setId(idFromPath);
            }
            if (entity.getId() == 0) {
                create(entity);
            } else {
                update(entity);
            }
            exchange.sendResponseHeaders(201, 0);
            exchange.close();
        }

        private void handleDelete(HttpExchange exchange) throws IOException {
            Integer id = extractId(exchange);
            if (id == null) {
                deleteAll();
            } else {
                deleteById(id);
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }
    }

    private class TasksHandler extends EntityHandler<Task> {
        protected Class<Task> getEntityClass() {
            return Task.class;
        }

        protected List<Task> getAll() {
            return manager.getTasks();
        }

        protected Task getById(int id) {
            return manager.getTask(id);
        }

        protected void create(Task entity) {
            manager.addNewTask(entity);
        }

        protected void update(Task entity) {
            manager.updateTask(entity);
        }

        protected void deleteAll() {
            manager.deleteTasks();
        }

        protected void deleteById(int id) {
            manager.deleteTask(id);
        }
    }

    private class SubtasksHandler extends EntityHandler<Subtask> {
        protected Class<Subtask> getEntityClass() {
            return Subtask.class;
        }

        protected List<Subtask> getAll() {
            return manager.getSubtasks();
        }

        protected Subtask getById(int id) {
            return manager.getSubtask(id);
        }

        protected void create(Subtask entity) {
            if (entity.getEpicId() == 0) throw new NotFoundException("epicId не передан или равен 0");
            manager.addNewSubtask(entity);
        }

        protected void update(Subtask entity) {
            manager.updateSubtask(entity);
        }

        protected void deleteAll() {
            manager.deleteSubtasks();
        }

        protected void deleteById(int id) {
            manager.deleteSubtask(id);
        }
    }

    private class EpicsHandler extends EntityHandler<Epic> {
        protected Class<Epic> getEntityClass() {
            return Epic.class;
        }

        protected List<Epic> getAll() {
            return manager.getEpics();
        }

        protected Epic getById(int id) {
            return manager.getEpic(id);
        }

        protected void create(Epic entity) {
            manager.addNewEpic(entity);
        }

        protected void update(Epic entity) {
            manager.updateEpic(entity);
        }

        protected void deleteAll() {
            manager.deleteEpics();
        }

        protected void deleteById(int id) {
            manager.deleteEpic(id);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");

                if ("GET".equals(method)
                        && parts.length == 4
                        && "epics".equals(parts[1])
                        && "subtasks".equals(parts[3])) {

                    int epicId = Integer.parseInt(parts[2]);
                    manager.getEpic(epicId);
                    String json = gson.toJson(manager.getEpicSubtasks(epicId));
                    sendText(exchange, json);
                    return;
                }

                super.handle(exchange);
            } catch (ManagerTimeIntersectionException | IllegalArgumentException e) {
                sendHasInteractions(exchange);
            } catch (NotFoundException e) {
                sendNotFound(exchange, e.getMessage());
            } catch (Exception e) {
                sendServerError(exchange);
            }
        }
    }

    private class HistoryHandler extends BaseHttpHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendServerError(exchange);
                return;
            }
            sendText(exchange, gson.toJson(manager.getHistory()));
        }
    }

    private class PrioritizedHandler extends BaseHttpHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendServerError(exchange);
                return;
            }
            sendText(exchange, gson.toJson(manager.getPrioritizedTasks()));
        }
    }
}
