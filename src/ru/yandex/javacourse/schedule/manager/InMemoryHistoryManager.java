package ru.yandex.javacourse.schedule.manager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ru.yandex.javacourse.schedule.tasks.Task;

/**
 * In memory history manager.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public class InMemoryHistoryManager implements HistoryManager {
	private final LinkedList<Task> history = new LinkedList<>();

	public static final int MAX_SIZE = 10;

	@Override
	public List<Task> getHistory() {
		return new ArrayList<>(history);
	}

	@Override
	public void addTask(Task task) {
		if (task == null) {
			return;
		}
		history.add(task);
		if (history.size() > MAX_SIZE) {
			history.removeFirst();
		}

	}
}
