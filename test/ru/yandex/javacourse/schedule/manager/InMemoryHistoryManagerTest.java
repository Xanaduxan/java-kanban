package ru.yandex.javacourse.schedule.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class InMemoryHistoryManagerTest {

    HistoryManager historyManager;

    @BeforeEach
    public void initHistoryManager(){
        historyManager = Managers.getDefaultHistory();
        for (Task t : new ArrayList<>(historyManager.getHistory())) {
            historyManager.remove(t.getId());
        }
    }

    @Test
    public void testHistoricVersions(){
        TaskManager taskManager = new InMemoryTaskManager();
        Task task = new Task("Test 1", "Testiong task 1", TaskStatus.NEW);

        int id = taskManager.addNewTask(task);

        assertEquals(0, taskManager.getHistory().size());

        taskManager.getTask(id);
        assertEquals(1, taskManager.getHistory().size(), "The first view should add an entry");

        task.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateTask(task);
        taskManager.getTask(id);

        assertEquals(1, taskManager.getHistory().size(), "A repeated view should not create a duplicate");
        assertEquals(TaskStatus.IN_PROGRESS, taskManager.getHistory().get(0).getStatus());
    }

    @Test
    public void testHistoricVersionsByPointer() {
        TaskManager taskManager = new InMemoryTaskManager();

        int id = taskManager.addNewTask(new Task("Test 1", "Testing task 1", TaskStatus.NEW));

        Task task = taskManager.getTask(id);
        assertEquals(1, taskManager.getHistory().size(), "An entry should appear in the history");

        assertEquals(task, taskManager.getHistory().get(0), "History should store the same object");
        assertEquals(TaskStatus.NEW, taskManager.getHistory().get(0).getStatus(), "The initial status is preserved");

        task.setStatus(TaskStatus.IN_PROGRESS);
        taskManager.updateTask(task);

        taskManager.getTask(id);
        assertEquals(1, taskManager.getHistory().size(), "A repeated view should not create a duplicate");
        assertEquals(task, taskManager.getHistory().get(0), "The reference in history should remain the same");
        assertEquals(TaskStatus.IN_PROGRESS, taskManager.getHistory().get(0).getStatus(),
                "The history should contain the updated status");
    }

    @Test
    void historyHasNoDuplicates() {
        TaskManager tm = new InMemoryTaskManager();
        int id = tm.addNewTask(new Task("A", "d", TaskStatus.NEW));

        tm.getTask(id);
        tm.getTask(id);

        assertEquals(1, tm.getHistory().size());
        assertEquals(id, tm.getHistory().get(0).getId());
    }

}