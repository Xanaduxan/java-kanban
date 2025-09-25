package ru.yandex.javacourse.schedule.tasks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EpicTest {

    @Test
    public void testEqualityById() {
        Epic e0 = new Epic(1, "Test 1", "Testing task 1");
        Epic e1 = new Epic(1, "Test 2", "Testing task 2");
        assertEquals(e0, e1, "task and subentities should be compared by id");
    }

    @Test
    public void testSubtaskUniqueIds() {
        Epic epic = new Epic(0, "Epic 1", "Testing epic 1");
        epic.addSubtaskId(1);
        epic.addSubtaskId(2);
        assertEquals(2, epic.subtaskIds.size(), "should add distinct subtask ids");
        epic.addSubtaskId(1);
        assertEquals(2, epic.subtaskIds.size(), "should not add same subtask id twice");
    }

    @Test
    public void testNotSelfAttaching() {
        Epic epic = new Epic(0, "Epic 1", "Testing epic 1");
        epic.addSubtaskId(0);
        assertEquals(0, epic.subtaskIds.size(), "epic should not add itself as subtask");
    }
}
