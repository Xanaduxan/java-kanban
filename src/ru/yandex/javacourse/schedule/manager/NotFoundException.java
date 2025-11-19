package ru.yandex.javacourse.schedule.manager;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}