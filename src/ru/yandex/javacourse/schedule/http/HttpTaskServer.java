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

    private static final int PORT = 8080;

    private final HttpServer httpServer;
    private final TaskManager manager;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Duration.class, new DurationAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create();

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
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            String query = uri.getQuery();

            System.out.println("Обработка " + method + " " + path + (query != null ? ("?" + query) : ""));

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

        private void handleGet(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();

            if (query == null) {
                List<T> all = getAll();
                String json = gson.toJson(all);
                sendText(exchange, json);
            } else {
                String[] parts = query.split("=");
                if (parts.length == 2 && "id".equals(parts[0])) {
                    int id = Integer.parseInt(parts[1]);
                    T entity = getById(id);
                    String json = gson.toJson(entity);
                    sendText(exchange, json);
                } else {
                    sendServerError(exchange);
                }
            }
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            T entity = gson.fromJson(body, getEntityClass());

            if (entity.getId() == 0) {
                create(entity);
            } else {
                update(entity);
            }

            exchange.sendResponseHeaders(201, 0);
            exchange.close();
        }

        private void handleDelete(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();

            if (query == null) {
                deleteAll();
            } else {
                String[] parts = query.split("=");
                if (parts.length == 2 && "id".equals(parts[0])) {
                    int id = Integer.parseInt(parts[1]);
                    deleteById(id);
                } else {
                    sendServerError(exchange);
                    return;
                }
            }

            exchange.sendResponseHeaders(201, 0);
            exchange.close();
        }
    }

    class TasksHandler extends EntityHandler<Task> {

        @Override
        protected Class<Task> getEntityClass() {
            return Task.class;
        }

        @Override
        protected List<Task> getAll() {
            return manager.getTasks();
        }

        @Override
        protected Task getById(int id) {
            return manager.getTask(id);
        }

        @Override
        protected void create(Task entity) {
            manager.addNewTask(entity);
        }

        @Override
        protected void update(Task entity) {
            manager.updateTask(entity);
        }

        @Override
        protected void deleteAll() {
            manager.deleteTasks();
        }

        @Override
        protected void deleteById(int id) {
            manager.deleteTask(id);
        }
    }

    class SubtasksHandler extends EntityHandler<Subtask> {

        @Override
        protected Class<Subtask> getEntityClass() {
            return Subtask.class;
        }

        @Override
        protected List<Subtask> getAll() {
            return manager.getSubtasks();
        }

        @Override
        protected Subtask getById(int id) {
            return manager.getSubtask(id);
        }

        @Override
        protected void create(Subtask entity) {
            if (entity.getEpicId() == 0) {
                throw new NotFoundException("epicId не передан или равен 0");
            }
            manager.addNewSubtask(entity);
        }

        @Override
        protected void update(Subtask entity) {
            manager.updateSubtask(entity);
        }

        @Override
        protected void deleteAll() {
            manager.deleteSubtasks();
        }

        @Override
        protected void deleteById(int id) {
            manager.deleteSubtask(id);
        }
    }

    class EpicsHandler extends EntityHandler<Epic> {

        @Override
        protected Class<Epic> getEntityClass() {
            return Epic.class;
        }

        @Override
        protected List<Epic> getAll() {
            return manager.getEpics();
        }

        @Override
        protected Epic getById(int id) {
            return manager.getEpic(id);
        }

        @Override
        protected void create(Epic entity) {
            manager.addNewEpic(entity);
        }

        @Override
        protected void update(Epic entity) {
            manager.updateEpic(entity);
        }

        @Override
        protected void deleteAll() {
            manager.deleteEpics();
        }

        @Override
        protected void deleteById(int id) {
            manager.deleteEpic(id);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            if ("GET".equals(method) && parts.length == 4 && "epics".equals(parts[1]) && "subtasks".equals(parts[3])) {

                try {
                    int epicId = Integer.parseInt(parts[2]);
                    manager.getEpic(epicId);
                    String json = gson.toJson(manager.getEpicSubtasks(epicId));
                    sendText(exchange, json);
                } catch (NumberFormatException e) {
                    sendServerError(exchange);
                } catch (NotFoundException e) {
                    sendNotFound(exchange, e.getMessage());
                }
                return;
            }

            super.handle(exchange);
        }
    }

    class HistoryHandler extends BaseHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();

            System.out.println("Обработка " + method + " " + uri.getPath());

            try {
                if (!"GET".equals(method)) {
                    sendServerError(exchange);
                    return;
                }

                String json = gson.toJson(manager.getHistory());
                sendText(exchange, json);
            } catch (Exception e) {
                sendServerError(exchange);
            }
        }
    }

    class PrioritizedHandler extends BaseHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();

            System.out.println("Обработка " + method + " " + uri.getPath());

            try {
                if (!"GET".equals(method)) {
                    sendServerError(exchange);
                    return;
                }

                String json = gson.toJson(manager.getPrioritizedTasks());
                sendText(exchange, json);
            } catch (Exception e) {
                sendServerError(exchange);
            }
        }
    }
}
