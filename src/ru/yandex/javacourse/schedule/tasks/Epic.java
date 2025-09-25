package ru.yandex.javacourse.schedule.tasks;

import static ru.yandex.javacourse.schedule.tasks.TaskStatus.NEW;

import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
	protected ArrayList<Integer> subtaskIds = new ArrayList<>();

	public Epic(int id, String name, String description) {
		super(id, name, description, NEW);
	}

	public Epic(String name, String description) {
		super(name, description, NEW);
	}

	public void addSubtaskId(int id) {
		subtaskIds.add(id);
	}

	public List<Integer> getSubtaskIds() {
		return subtaskIds;
	}

	public void cleanSubtaskIds() {
		subtaskIds.clear();
	}

	public void removeSubtask(int id) {
		subtaskIds.remove(Integer.valueOf(id));
	}

	@Override
	public String toString() {
		return "Epic{" +
				"id=" + id +
				", name='" + name + '\'' +
				", status=" + status +
				", description='" + description + '\'' +
				", subtaskIds=" + subtaskIds +
				'}';
	}
}
