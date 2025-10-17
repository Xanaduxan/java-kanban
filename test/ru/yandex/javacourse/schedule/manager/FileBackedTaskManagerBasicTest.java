package ru.yandex.javacourse.schedule.manager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerBasicTest {

    private File tempFile;
    private FileBackedTaskManager manager;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = File.createTempFile("tasks", ".csv");
        Files.writeString(tempFile.toPath(), "", StandardCharsets.UTF_8);
        manager = new FileBackedTaskManager(tempFile.toPath());
    }

    @AfterEach
    void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            assertTrue(tempFile.delete());
        }
    }

    @Test
    void testSaveAndLoadSingleTaskRoundTrip() {
        Task t = new Task("T1", "D1", TaskStatus.NEW);
        int id = manager.addNewTask(t);
        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, loaded.getTasks().size());
        Task loadedTask = loaded.getTasks().getFirst();
        assertEquals(id, loadedTask.getId());
        assertEquals("T1", loadedTask.getName());
        assertEquals("D1", loadedTask.getDescription());
        assertEquals(TaskStatus.NEW, loadedTask.getStatus());

        String csv = readCsv();
        assertTrue(csv.startsWith("id,type,name,status,description,epic"));
        assertEquals(2, csv.split("\\R").length);
    }

    @Test
    void testSaveAndLoadEpicWithSubtasksLinksAndStatusRestored() {
        int epicId = manager.addNewEpic(new Epic("E1", "ED1"));
        int s1 = manager.addNewSubtask(new Subtask("S1", "SD1", TaskStatus.NEW, epicId));
        int s2 = manager.addNewSubtask(new Subtask("S2", "SD2", TaskStatus.DONE, epicId));

        assertEquals(TaskStatus.IN_PROGRESS, manager.getEpic(epicId).getStatus());

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, loaded.getEpics().size());
        assertEquals(2, loaded.getSubtasks().size());

        Epic e = loaded.getEpics().getFirst();
        assertEquals("E1", e.getName());
        assertEquals(2, e.getSubtaskIds().size());
        assertEquals(TaskStatus.IN_PROGRESS, e.getStatus());

        Subtask ls1 = loaded.getSubtask(s1);
        Subtask ls2 = loaded.getSubtask(s2);
        assertNotNull(ls1);
        assertNotNull(ls2);

        assertEquals("S1", ls1.getName());
        assertEquals(TaskStatus.NEW, ls1.getStatus());
        assertEquals(e.getId(), ls1.getEpicId());

        assertEquals("S2", ls2.getName());
        assertEquals(TaskStatus.DONE, ls2.getStatus());
        assertEquals(e.getId(), ls2.getEpicId());
    }


    @Test
    void testAutosaveOnUpdateAndDeleteReflectedInFile() {
        int tid = manager.addNewTask(new Task("T", "D", TaskStatus.NEW));

        Task t = manager.getTask(tid);
        t.setStatus(TaskStatus.DONE);
        manager.updateTask(t);

        String csvAfterUpdate = readCsv();
        assertTrue(csvAfterUpdate.contains(",TASK,T,") && csvAfterUpdate.contains(",DONE,"));

        manager.deleteTask(tid);

    }

    private String readCsv() {
        try {
            return Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail(e);
            return "";
        }
    }
}
