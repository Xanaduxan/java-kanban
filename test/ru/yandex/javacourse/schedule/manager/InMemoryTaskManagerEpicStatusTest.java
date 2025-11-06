package ru.yandex.javacourse.schedule.manager;

public class InMemoryTaskManagerEpicStatusTest extends TaskManagerTest<InMemoryTaskManager> {

    @Override
    protected InMemoryTaskManager createManager() {
        return new InMemoryTaskManager();
    }
}
