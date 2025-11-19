package ru.yandex.javacourse.schedule.http.handler;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BaseHttpHandler {


    protected void sendText(HttpExchange h, String text) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(200, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    protected void sendNotFound(HttpExchange h, String message) throws IOException {
        byte[] resp = message.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(404, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }


    protected void sendHasInteractions(HttpExchange h) throws IOException {
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(406, 0);
        h.close();
    }

    protected void sendServerError(HttpExchange h) throws IOException {
        h.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        h.sendResponseHeaders(500, 0);
        h.close();
    }


}
