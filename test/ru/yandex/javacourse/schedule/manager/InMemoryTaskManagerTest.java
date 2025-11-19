package ru.yandex.javacourse.schedule.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InMemoryTaskManagerTest {

    private TaskManager manager;

    @BeforeEach
    public void initManager() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void testAddTask() {
        Task task = new Task("Test 1", "Testing task 1", TaskStatus.NEW);

        int id = manager.addNewTask(task);

        Task fromManager = manager.getTask(id);
        assertEquals(task, fromManager);
    }

    @Test
    void testAddTaskWithId() {
        Task task = new Task(42, "Test 1", "Testing task 1", TaskStatus.NEW);

        int id = manager.addNewTask(task);

        Task fromManager = manager.getTask(id);
        assertEquals(task, fromManager);
    }


}
