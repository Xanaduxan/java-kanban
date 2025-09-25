package ru.yandex.javacourse.schedule.tasks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SubtaskTest {

    @Test
    public void testEqualityById(){
        Subtask s0 = new Subtask(1, "Test 1", "Testing task 1", TaskStatus.NEW, 1);
        Subtask s1 = new Subtask(1, "Test 2", "Testing task 2", TaskStatus.IN_PROGRESS, 2);
        assertEquals(s0, s1, "task entities should be compared by id");
    }

    @Test
    public void testNotSelfAttaching() {
        Subtask subtask = new Subtask(1, "Subtask 1", "Testing subtask 1", TaskStatus.NEW, 1);
        assertNotEquals(subtask.id, subtask.epicId, "subtask cannot be attached to itself");
    }
}
