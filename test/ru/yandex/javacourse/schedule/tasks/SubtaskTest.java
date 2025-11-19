package ru.yandex.javacourse.schedule.tasks;

import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.manager.InMemoryTaskManager;
import ru.yandex.javacourse.schedule.manager.TaskManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SubtaskTest {

    @Test
    public void testEqualityById() {
        Subtask s0 = new Subtask(1, "Test 1", "Testing task 1", TaskStatus.NEW, 1);
        Subtask s1 = new Subtask(1, "Test 2", "Testing task 2", TaskStatus.IN_PROGRESS, 2);
        assertEquals(s0, s1, "task entities should be compared by id");
    }

    @Test
    public void deleteSubtask_removesFromStoreEpicAndHistory() {
        TaskManager taskManager = new InMemoryTaskManager();

        int epicId = taskManager.addNewEpic(new Epic("E", "d"));
        Integer s1 = taskManager.addNewSubtask(new Subtask("S1", "d1", TaskStatus.NEW, epicId));

        taskManager.getEpic(epicId);
        taskManager.getSubtask(s1);

        taskManager.deleteSubtask(s1);

        boolean found = false;
        for (Subtask s : taskManager.getSubtasks()) {
            if (s.getId() == s1) {
                found = true;
                break;
            }
        }
        assertFalse(found);

        Epic epic = taskManager.getEpic(epicId);
        boolean idInEpic = false;
        for (int id : epic.getSubtaskIds()) {
            if (id == s1) {
                idInEpic = true;
                break;
            }
        }
        assertFalse(idInEpic);

        boolean inHistory = false;
        for (Task t : taskManager.getHistory()) {
            if (t.getId() == s1) {
                inHistory = true;
                break;
            }
        }
        assertFalse(inHistory);
    }

    @Test
    public void deletedSubtask_doesNotKeepOldId_andGetsNewIdWhenReadded() {
        TaskManager taskManager = new InMemoryTaskManager();

        int epicId = taskManager.addNewEpic(new Epic("E", "d"));

        Subtask s = new Subtask("S1", "d1", TaskStatus.NEW, epicId);

        Integer oldId = taskManager.addNewSubtask(s);
        assertEquals(oldId.intValue(), s.getId(), "ID must be assigned on add");

        taskManager.getEpic(epicId);
        taskManager.getSubtask(oldId);

        taskManager.deleteSubtask(oldId);

        boolean found = false;
        for (Subtask st : taskManager.getSubtasks()) {
            if (st.getId() == oldId) {
                found = true;
                break;
            }
        }
        assertFalse(found, "Deleted subtask must not remain in storage");

        Epic epic = taskManager.getEpic(epicId);
        boolean idInEpic = false;
        for (int id : epic.getSubtaskIds()) {
            if (id == oldId) {
                idInEpic = true;
                break;
            }
        }
        assertFalse(idInEpic, "Epic must not contain a stale subtask id");

        boolean inHistory = false;
        for (Task t : taskManager.getHistory()) {
            if (t.getId() == oldId) {
                inHistory = true;
                break;
            }
        }
        assertFalse(inHistory, "Deleted subtask must not remain in history");

        Integer newId = taskManager.addNewSubtask(s);
        assertNotEquals(oldId, newId, "Re-added subtask must get a new id");
        assertEquals(newId.intValue(), s.getId(), "The subtask object must not 'keep' its old id internally");
    }

}
