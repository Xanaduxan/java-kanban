package ru.yandex.javacourse.schedule.manager;

import java.util.List;

import ru.yandex.javacourse.schedule.tasks.Task;

/**
 * History manager.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public interface HistoryManager {
	void add(Task task);
    void remove (int id);
    List<Task> getHistory();
}
