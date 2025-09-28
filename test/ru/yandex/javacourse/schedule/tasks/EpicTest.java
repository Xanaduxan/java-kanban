package ru.yandex.javacourse.schedule.tasks;

import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.manager.InMemoryTaskManager;
import ru.yandex.javacourse.schedule.manager.TaskManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EpicTest {

    @Test
    public void testEqualityById() {
        Epic e0 = new Epic(1, "Test 1", "Testing task 1");
        Epic e1 = new Epic(1, "Test 2", "Testing task 2");
        assertEquals(e0, e1, "task and subentities should be compared by id");
    }

    @Test
    public void testSubtaskUniqueIds_viaManager() {
        TaskManager manager = new InMemoryTaskManager();

        int epicId = manager.addNewEpic(new Epic("Epic 1", "Testing epic 1"));
        Integer s1 = manager.addNewSubtask(new Subtask("S1", "d1", TaskStatus.NEW, epicId));
        Integer s2 = manager.addNewSubtask(new Subtask("S2", "d2", TaskStatus.NEW, epicId));

        Epic epic = manager.getEpic(epicId);


        assertEquals(2, epic.getSubtaskIds().size(), "manager should add distinct subtask ids");
        assertTrue(epic.getSubtaskIds().containsAll(List.of(s1, s2)));


        manager.deleteSubtask(s1);
        epic = manager.getEpic(epicId);
        assertFalse(epic.getSubtaskIds().contains(s1), "deleted subtask id must be removed from epic");
        assertEquals(1, epic.getSubtaskIds().size());
    }



}
