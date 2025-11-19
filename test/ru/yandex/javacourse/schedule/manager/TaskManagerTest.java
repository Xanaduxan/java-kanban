package ru.yandex.javacourse.schedule.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.tasks.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.yandex.javacourse.schedule.tasks.TaskStatus.*;

public abstract class TaskManagerTest<T extends TaskManager> {

    private T manager;

    protected abstract T createManager();

    @BeforeEach
    void setUp() {
        manager = createManager();
    }


    @Test
    void epic_allSubtasksNew_epicIsNEW() {
        int epicId = manager.addNewEpic(new Epic("Epic", "test"));
        manager.addNewSubtask(new Subtask("S1", "d", NEW, epicId));
        manager.addNewSubtask(new Subtask("S2", "d", NEW, epicId));
        assertEquals(NEW, manager.getEpic(epicId).getStatus());
    }

    @Test
    void epic_allSubtasksDone_epicIsDONE() {
        int epicId = manager.addNewEpic(new Epic("Epic", "test"));
        manager.addNewSubtask(new Subtask("S1", "d", DONE, epicId));
        manager.addNewSubtask(new Subtask("S2", "d", DONE, epicId));
        assertEquals(DONE, manager.getEpic(epicId).getStatus());
    }

    @Test
    void epic_mixedNewAndDone_epicIsIN_PROGRESS() {
        int epicId = manager.addNewEpic(new Epic("Epic", "test"));
        manager.addNewSubtask(new Subtask("S1", "d", NEW, epicId));
        manager.addNewSubtask(new Subtask("S2", "d", DONE, epicId));
        assertEquals(IN_PROGRESS, manager.getEpic(epicId).getStatus());
    }

    @Test
    void epic_withInProgressSubtask_epicIsIN_PROGRESS() {
        int epicId = manager.addNewEpic(new Epic("Epic", "test"));
        manager.addNewSubtask(new Subtask("S1", "d", IN_PROGRESS, epicId));
        assertEquals(IN_PROGRESS, manager.getEpic(epicId).getStatus());
    }

    @Test
    void subtaskMustBelongToExistingEpic() {
        assertThrows(NotFoundException.class, () ->
                manager.addNewSubtask(new Subtask("S1", "d", NEW, 999))
        );
    }


    @Test
    void epicShouldContainAddedSubtasks() {
        int epicId = manager.addNewEpic(new Epic("Epic", "desc"));
        int subId = manager.addNewSubtask(new Subtask("S1", "d", NEW, epicId));
        Epic epic = manager.getEpic(epicId);
        assertTrue(epic.getSubtaskIds().contains(subId));
    }


    @Test
    void tasksWithIntersectingTimeShouldThrowException() {
        Task t1 = new Task("T1", "desc", NEW,
                Duration.ofMinutes(60),
                LocalDateTime.of(2025, 11, 6, 10, 0));
        manager.addNewTask(t1);

        Task t2 = new Task("T2", "desc", NEW,
                Duration.ofMinutes(30),
                LocalDateTime.of(2025, 11, 6, 10, 30));

        assertThrows(ManagerTimeIntersectionException.class, () -> manager.addNewTask(t2),
                "Должно выбрасываться исключение при пересечении по времени");
    }

    @Test
    void tasksWithoutIntersectionCanBeAdded() {
        Task t1 = new Task("T1", "desc", NEW,
                Duration.ofMinutes(60),
                LocalDateTime.of(2025, 11, 6, 10, 0));
        manager.addNewTask(t1);

        Task t2 = new Task("T2", "desc", NEW,
                Duration.ofMinutes(30),
                LocalDateTime.of(2025, 11, 6, 11, 0));
        assertDoesNotThrow(() -> manager.addNewTask(t2));
    }


    @Test
    void canCreateAndGetTask() {
        Task t = new Task("Test", "desc", NEW);
        int id = manager.addNewTask(t);
        assertNotNull(manager.getTask(id));
    }

    @Test
    void canDeleteTask() {
        Task t = new Task("T", "D", NEW);
        int id = manager.addNewTask(t);
        manager.deleteTask(id);
        assertTrue(manager.getTasks().isEmpty());
    }

    @Test
    void getHistoryInitiallyEmpty() {
        assertTrue(manager.getHistory().isEmpty());
    }

    @Test
    void getPrioritizedTasksReturnsSortedList() {
        Task t1 = new Task("T1", "desc", NEW,
                Duration.ofMinutes(30),
                LocalDateTime.of(2025, 11, 6, 9, 0));
        Task t2 = new Task("T2", "desc", NEW,
                Duration.ofMinutes(30),
                LocalDateTime.of(2025, 11, 6, 8, 0));
        manager.addNewTask(t1);
        manager.addNewTask(t2);
        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(t2, prioritized.get(0));
        assertEquals(t1, prioritized.get(1));
    }
}
