package ru.yandex.javacourse.schedule.manager;

import java.util.List;

import ru.yandex.javacourse.schedule.tasks.Task;

/**
 * History manager.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public interface HistoryManager {
	List<Task> getHistory();

	void addTask(Task task);
}
