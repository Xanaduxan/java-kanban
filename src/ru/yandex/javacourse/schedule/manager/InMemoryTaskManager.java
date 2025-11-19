package ru.yandex.javacourse.schedule.manager;

import static ru.yandex.javacourse.schedule.tasks.TaskStatus.IN_PROGRESS;
import static ru.yandex.javacourse.schedule.tasks.TaskStatus.NEW;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

public class InMemoryTaskManager implements TaskManager {

    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected int generatorId = 0;
    private final HistoryManager historyManager = Managers.getDefaultHistory();
    private final TreeSet<Task> prioritizedTasks = new TreeSet<>(Comparator.comparing(Task::getStartTime)
            .thenComparingInt(Task::getId));


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
            return tasks;
        }
        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Task getTask(int id) {
        final Task task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException("Задача с id=" + id + " не найдена");
        }
        historyManager.add(task);
        return task;
    }

    @Override
    public Subtask getSubtask(int id) {
        final Subtask subtask = subtasks.get(id);
        if (subtask == null) {
            throw new NotFoundException("Подзадача с id=" + id + " не найдена");
        }
        historyManager.add(subtask);
        return subtask;
    }

    @Override
    public Epic getEpic(int id) {
        final Epic epic = epics.get(id);
        if (epic == null) {
            throw new NotFoundException("Эпик с id=" + id + " не найден");
        }
        historyManager.add(epic);
        return epic;
    }

    @Override
    public int addNewTask(Task task) {
        ensureNoOverlap(task);

        final int id = ++generatorId;
        task.setId(id);
        tasks.put(id, task);
        addToPrioritizedIfNeeded(task);
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
        ensureNoOverlap(subtask);
        final int epicId = subtask.getEpicId();
        Epic epic = epics.get(epicId);
        if (epic == null) {
            throw new NotFoundException("Эпик с id=" + epicId + " не найден");
        }
        final int id = ++generatorId;
        subtask.setId(id);
        subtasks.put(id, subtask);
        epic.addSubtaskId(subtask.getId());
        updateEpicStatus(epicId);
        updateEpicTime(epic.getId());
        addToPrioritizedIfNeeded(subtask);
        return id;
    }


    @Override
    public void updateTask(Task task) {
        ensureNoOverlap(task);
        final int id = task.getId();
        final Task savedTask = tasks.get(id);
        if (savedTask == null) {
            throw new NotFoundException("Задача с id=" + id + " не найдена");
        }
        removeFromPrioritizedIfPresent(savedTask);
        tasks.put(id, task);
        addToPrioritizedIfNeeded(task);
    }

    @Override
    public void updateEpic(Epic epic) {
        final Epic savedEpic = epics.get(epic.getId());
        if (savedEpic == null) {
            throw new NotFoundException("Эпик с id=" + epic.getId() + " не найден");
        }
        savedEpic.setName(epic.getName());
        savedEpic.setDescription(epic.getDescription());
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        ensureNoOverlap(subtask);
        final int id = subtask.getId();
        final int epicId = subtask.getEpicId();
        final Subtask savedSubtask = subtasks.get(id);
        if (savedSubtask == null) {
            throw new NotFoundException("Подзадача с id=" + id + " не найдена");
        }
        final Epic epic = epics.get(epicId);
        if (epic == null) {
            throw new NotFoundException("Эпик с id=" + epicId + " не найден");
        }
        removeFromPrioritizedIfPresent(savedSubtask);
        subtasks.put(id, subtask);
        addToPrioritizedIfNeeded(subtask);
        updateEpicStatus(epicId);
        updateEpicTime(epic.getId());
    }

    @Override
    public void deleteTask(int id) {
        Task task = tasks.remove(id);
        if (task == null) {
            throw new NotFoundException("Задача с id=" + id + " не найдена");
        }
        removeFromPrioritizedIfPresent(task);
        historyManager.remove(id);
    }

    @Override
    public void deleteEpic(int id) {
        final Epic epic = epics.get(id);
        if (epic == null) {
            throw new NotFoundException("Эпик с id=" + id + " не найден");
        }

        epic.getSubtaskIds().stream()
                .map(subtasks::remove)
                .filter(Objects::nonNull)
                .forEach(subtask -> {
                    removeFromPrioritizedIfPresent(subtask);
                    historyManager.remove(subtask.getId());
                });

        epics.remove(id);
        historyManager.remove(id);
    }

    @Override
    public void deleteSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask == null) {
            throw new NotFoundException("Подзадача с id=" + id + " не найдена");
        }
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            epic.removeSubtask(id);
            updateEpicStatus(epic.getId());
            updateEpicTime(epic.getId());
        }

        removeFromPrioritizedIfPresent(subtask);
        subtasks.remove(id);
        historyManager.remove(id);
    }


    @Override
    public void deleteTasks() {

        prioritizedTasks.stream()
                .filter(t -> !(t instanceof Subtask))
                .toList()
                .forEach(prioritizedTasks::remove);


        new ArrayList<>(tasks.keySet())
                .forEach(historyManager::remove);

        tasks.clear();
    }

    @Override
    public void deleteSubtasks() {

        prioritizedTasks.stream()
                .filter(task -> task instanceof Subtask)
                .toList()
                .forEach(prioritizedTasks::remove);
        new ArrayList<>(subtasks.keySet())
                .forEach(historyManager::remove);


        epics.values().forEach(epic -> {
            epic.cleanSubtaskIds();
            updateEpicStatus(epic.getId());
            updateEpicTime(epic.getId());
        });

        subtasks.clear();
    }

    @Override
    public void deleteEpics() {

        List<Task> toRemove = prioritizedTasks.stream()
                .filter(t -> t instanceof Subtask)
                .toList();
        toRemove.forEach(prioritizedTasks::remove);

        subtasks.keySet().forEach(historyManager::remove);

        epics.keySet().forEach(historyManager::remove);

        epics.clear();
        subtasks.clear();
    }


    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }

    protected void updateEpicStatus(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) return;

        List<Integer> ids = epic.getSubtaskIds();
        if (ids.isEmpty()) {
            epic.setStatus(NEW);
            return;
        }


        Optional<TaskStatus> reduced = ids.stream()
                .map(subtasks::get)
                .map(Subtask::getStatus)
                .reduce((acc, st) -> (acc == st && acc != IN_PROGRESS) ? acc : IN_PROGRESS);

        epic.setStatus(reduced.orElse(NEW));
    }


    protected void updateEpicTime(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) return;

        List<Integer> subs = epic.getSubtaskIds();
        if (subs.isEmpty()) {
            epic.setDuration(null);
            epic.setStartTime(null);
            epic.setEndTime(null);
            return;
        }

        List<Subtask> validSubs = subs.stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .filter(st -> st.getStartTime() != null && st.getDuration() != null)
                .toList();

        if (validSubs.isEmpty()) {
            epic.setDuration(null);
            epic.setStartTime(null);
            epic.setEndTime(null);
            return;
        }

        Duration durationCounter = validSubs.stream()
                .map(Subtask::getDuration)
                .reduce(Duration::plus)
                .orElse(null);

        LocalDateTime minTime = validSubs.stream()
                .map(Subtask::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime maxTime = validSubs.stream()
                .map(Subtask::getEndTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        epic.setDuration(durationCounter);
        epic.setStartTime(minTime);
        epic.setEndTime(maxTime);
    }


    protected void addToPrioritizedIfNeeded(Task t) {
        if (t instanceof Epic) return;
        if (t.getStartTime() == null) return;
        prioritizedTasks.add(t);
    }

    private void removeFromPrioritizedIfPresent(Task t) {
        if (t instanceof Epic) return;
        if (t.getStartTime() == null) return;
        prioritizedTasks.remove(t);
    }


    private boolean intersects(Task a, Task b) {
        if (a == null || b == null) return false;
        if (a.getStartTime() == null || a.getDuration() == null) return false;
        if (b.getStartTime() == null || b.getDuration() == null) return false;

        LocalDateTime aEnd = a.getEndTime();
        LocalDateTime bEnd = b.getEndTime();

        return a.getStartTime().isBefore(bEnd) && b.getStartTime().isBefore(aEnd);
    }


    private boolean hasIntersections(Task candidateTask) {
        return prioritizedTasks.stream().filter(t -> t.getId() != candidateTask.getId()).anyMatch(other -> intersects(candidateTask, other));
    }


    private void ensureNoOverlap(Task t) {
        if (t == null) return;
        if (t instanceof Epic) return;
        if (t.getStartTime() == null || t.getDuration() == null) return;
        if (hasIntersections(t)) {
            throw new ManagerTimeIntersectionException("Пересечение по времени с другой задачей. id=" + t.getId());
        }
    }

}
