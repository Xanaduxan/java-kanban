package ru.yandex.javacourse.schedule.manager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default managers.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public class Managers {

    private static final Path DEFAULT_FILE = Paths.get("tasks.csv");

    public static TaskManager getDefault() {
        if (Files.exists(DEFAULT_FILE)) {
            return FileBackedTaskManager.loadFromFile(DEFAULT_FILE.toFile());
        }


        return new FileBackedTaskManager(DEFAULT_FILE);
    }

    public static FileBackedTaskManager getDefaultFileBackedTask(Path file) {
        return new FileBackedTaskManager(file);
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
