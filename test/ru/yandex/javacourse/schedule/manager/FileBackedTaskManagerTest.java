package ru.yandex.javacourse.schedule.manager;

import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {

    @TempDir
    Path tempDir;

    @Override
    protected FileBackedTaskManager createManager() {
        return new FileBackedTaskManager(tempDir.resolve("tasks.csv"));
    }
}
