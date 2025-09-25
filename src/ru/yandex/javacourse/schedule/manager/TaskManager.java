package ru.yandex.javacourse.schedule.manager;

import java.util.List;

import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;

/**
 * Task manager.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public interface TaskManager {
	List<Task> getTasks();

	List<Subtask> getSubtasks();

	List<Epic> getEpics();

	List<Subtask> getEpicSubtasks(int epicId);

	Task getTask(int id);

	Subtask getSubtask(int id);

	Epic getEpic(int id);

	int addNewTask(Task task);

	int addNewEpic(Epic epic);

	Integer addNewSubtask(Subtask subtask);

	void updateTask(Task task);

	void updateEpic(Epic epic);

	void updateSubtask(Subtask subtask);

	void deleteTask(int id);

	void deleteEpic(int id);

	void deleteSubtask(int id);

	void deleteTasks();

	void deleteSubtasks();

	void deleteEpics();

	List<Task> getHistory();
}
