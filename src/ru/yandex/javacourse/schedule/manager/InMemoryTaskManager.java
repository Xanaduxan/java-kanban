package ru.yandex.javacourse.schedule.manager;

import static ru.yandex.javacourse.schedule.tasks.TaskStatus.IN_PROGRESS;
import static ru.yandex.javacourse.schedule.tasks.TaskStatus.NEW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

public class InMemoryTaskManager implements TaskManager {

	private final Map<Integer, Task> tasks = new HashMap<>();
	private final Map<Integer, Epic> epics = new HashMap<>();
	private final Map<Integer, Subtask> subtasks = new HashMap<>();
	private int generatorId = 0;
	private final HistoryManager historyManager = Managers.getDefaultHistory();


	@Override
	public ArrayList<Task> getTasks() {
		return new ArrayList<>(this.tasks.values());
	}

	@Override
	public ArrayList<Subtask> getSubtasks() {
		return new ArrayList<>(subtasks.values());
	}

	@Override
	public ArrayList<Epic> getEpics() {
		return new ArrayList<>(epics.values());
	}

	@Override
	public ArrayList<Subtask> getEpicSubtasks(int epicId) {
		ArrayList<Subtask> tasks = new ArrayList<>();
		Epic epic = epics.get(epicId);
		if (epic == null) {
			return null;
		}
		for (int id : epic.getSubtaskIds()) {
			tasks.add(subtasks.get(id));
		}
		return tasks;
	}

	@Override
	public Task getTask(int id) {
		final Task task = tasks.get(id);
		historyManager.addTask(task);
		return task;
	}

	@Override
	public Subtask getSubtask(int id) {
		final Subtask subtask = subtasks.get(id);
		historyManager.addTask(subtask);
		return subtask;
	}

	@Override
	public Epic getEpic(int id) {
		final Epic epic = epics.get(id);
		historyManager.addTask(epic);
		return epic;
	}

	@Override
	public int addNewTask(Task task) {
		final int id = ++generatorId;
		task.setId(id);
		tasks.put(id, task);
		return id;
	}

	@Override
	public int addNewEpic(Epic epic) {
		final int id = ++generatorId;
		epic.setId(id);
		epics.put(id, epic);
		return id;

	}

	@Override
	public Integer addNewSubtask(Subtask subtask) {
		final int epicId = subtask.getEpicId();
		Epic epic = epics.get(epicId);
		if (epic == null) {
			return null;
		}
		final int id = ++generatorId;
		subtask.setId(id);
		subtasks.put(id, subtask);
		epic.addSubtaskId(subtask.getId());
		updateEpicStatus(epicId);
		return id;
	}

	@Override
	public void updateTask(Task task) {
		final int id = task.getId();
		final Task savedTask = tasks.get(id);
		if (savedTask == null) {
			return;
		}
		tasks.put(id, task);
	}

	@Override
	public void updateEpic(Epic epic) {
		final Epic savedEpic = epics.get(epic.getId());
		savedEpic.setName(epic.getName());
		savedEpic.setDescription(epic.getDescription());
	}

	@Override
	public void updateSubtask(Subtask subtask) {
		final int id = subtask.getId();
		final int epicId = subtask.getEpicId();
		final Subtask savedSubtask = subtasks.get(id);
		if (savedSubtask == null) {
			return;
		}
		final Epic epic = epics.get(epicId);
		if (epic == null) {
			return;
		}
		subtasks.put(id, subtask);
		updateEpicStatus(epicId);
	}

	@Override
	public void deleteTask(int id) {
		tasks.remove(id);
	}

	@Override
	public void deleteEpic(int id) {
		final Epic epic = epics.remove(id);
		for (Integer subtaskId : epic.getSubtaskIds()) {
			subtasks.remove(subtaskId);
		}
	}

	@Override
	public void deleteSubtask(int id) {
		Subtask subtask = subtasks.remove(id);
		if (subtask == null) {
			return;
		}
		Epic epic = epics.get(subtask.getEpicId());
		epic.removeSubtask(id);
		updateEpicStatus(epic.getId());
	}

	@Override
	public void deleteTasks() {
		tasks.clear();
	}

	@Override
	public void deleteSubtasks() {
		for (Epic epic : epics.values()) {
			epic.cleanSubtaskIds();
			updateEpicStatus(epic.getId());
		}
		subtasks.clear();
	}

	@Override
	public void deleteEpics() {
		epics.clear();
		subtasks.clear();
	}

	@Override
	public List<Task> getHistory() {
		return historyManager.getHistory();
	}

	private void updateEpicStatus(int epicId) {
		Epic epic = epics.get(epicId);
		List<Integer> subs = epic.getSubtaskIds();
		if (subs.isEmpty()) {
			epic.setStatus(NEW);
			return;
		}
		TaskStatus status = null;
		for (int id : subs) {
			final Subtask subtask = subtasks.get(id);
			if (status == null) {
				status = subtask.getStatus();
				continue;
			}

			if (status == subtask.getStatus()
					&& status != IN_PROGRESS) {
				continue;
			}
			epic.setStatus(IN_PROGRESS);
			return;
		}
		epic.setStatus(status);
	}
}
