package ru.yandex.javacourse.schedule.tasks;

import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.manager.InMemoryTaskManager;
import ru.yandex.javacourse.schedule.manager.TaskManager;

import static org.junit.jupiter.api.Assertions.*;

public class TaskTest {

    @Test
    public void testEqualityById() {
        Task t0 = new Task(1, "Test 1", "Testing task 1", TaskStatus.NEW);
        Task t1 = new Task(1, "Test 2", "Testing task 2", TaskStatus.IN_PROGRESS);
        assertEquals(t0, t1, "task entities should be compared by id");
    }


    @Test
    void deleteSubtaskCleansAll() {
        TaskManager taskManager = new InMemoryTaskManager();
        int epicId = taskManager.addNewEpic(new Epic("E", "d"));
        Integer s1 = taskManager.addNewSubtask(new Subtask("S1", "d1", TaskStatus.NEW, epicId));


        taskManager.getEpic(epicId);
        taskManager.getSubtask(s1);

        taskManager.deleteSubtask(s1);


        boolean found = false;
        for (Subtask st : taskManager.getSubtasks()) {
            if (st.getId() == s1) {
                found = true;
                break;
            }
        }
        assertFalse(found, "Not in storage");


        boolean idInEpic = false;
        for (int id : taskManager.getEpic(epicId).getSubtaskIds()) {
            if (id == s1) {
                idInEpic = true;
                break;
            }
        }
        assertFalse(idInEpic, "Not in epic");


        boolean inHistory = false;
        for (Task t : taskManager.getHistory()) {
            if (t.getId() == s1) {
                inHistory = true;
                break;
            }
        }
        assertFalse(inHistory, "Not in history");
    }
}
