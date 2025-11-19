package ru.yandex.javacourse.schedule.manager;

import ru.yandex.javacourse.schedule.tasks.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class FileBackedTaskManager extends InMemoryTaskManager {

    private final Path file;
    private static final String HEADER = "id,type,name,status,description,epic,duration,startTime";

    public FileBackedTaskManager(Path file) {
        this.file = file;
    }

    @Override
    public int addNewEpic(Epic epic) {
        int id = super.addNewEpic(epic);
        save();
        return id;
    }

    @Override
    public int addNewTask(Task task) {
        int id = super.addNewTask(task);
        save();
        return id;
    }

    @Override
    public Integer addNewSubtask(Subtask subtask) {
        Integer id = super.addNewSubtask(subtask);
        save();
        return id;
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteTask(int id) {
        super.deleteTask(id);
        save();
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
        save();
    }


    @Override
    public void deleteSubtask(int id) {
        super.deleteSubtask(id);
        save();
    }

    @Override
    public void deleteTasks() {
        super.deleteTasks();
        save();
    }

    @Override
    public void deleteSubtasks() {
        super.deleteSubtasks();
        save();
    }

    @Override
    public void deleteEpics() {
        super.deleteEpics();
        save();
    }

    public static FileBackedTaskManager loadFromFile(File src) {
        Path path = src.toPath();
        FileBackedTaskManager manager = new FileBackedTaskManager(path);

        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = bufferedReader.readLine();
            if (header == null) return manager;
            if (!HEADER.equals(header.trim())) {
                throw new ManagerSaveException("Неверный заголовок CSV: " + header);
            }

            int maxId = 0;
            List<Subtask> buffer = new ArrayList<>();

            String line = bufferedReader.readLine();
            while (line != null) {
                if (!line.isBlank()) {
                    Task parsed = manager.fromString(line);

                    if (parsed instanceof Epic epic) {
                        manager.epics.put(epic.getId(), epic);
                        if (epic.getId() > maxId) maxId = epic.getId();
                    } else if (parsed instanceof Subtask subtask) {
                        buffer.add(subtask);
                        if (subtask.getId() > maxId) maxId = subtask.getId();
                    } else {
                        manager.tasks.put(parsed.getId(), parsed);
                        if (parsed.getId() > maxId) maxId = parsed.getId();
                    }
                }
                line = bufferedReader.readLine();
            }

            buffer.forEach(subtask -> {
                Epic epic = manager.epics.get(subtask.getEpicId());
                if (epic == null) {
                    throw new ManagerSaveException("Нет эпика " + subtask.getEpicId() + " для сабтаска " + subtask.getId());
                }
                manager.subtasks.put(subtask.getId(), subtask);
                epic.addSubtaskId(subtask.getId());
            });


            manager.epics.values().forEach(epic -> {
                manager.updateEpicStatus(epic.getId());
                manager.updateEpicTime(epic.getId());
            });

            manager.tasks.values().forEach(manager::addToPrioritizedIfNeeded);
            manager.subtasks.values().forEach(manager::addToPrioritizedIfNeeded);

            manager.generatorId = maxId;
        } catch (IOException e) {
            throw new ManagerSaveException("Не удалось прочитать файл: " + path, e);
        }

        return manager;
    }


    private void save() {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            bufferedWriter.write(HEADER);
            bufferedWriter.newLine();


            String tasksBlock = tasks.values().stream()
                    .sorted(Comparator.comparingInt(Task::getId))
                    .map(this::toString)
                    .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
            bufferedWriter.write(tasksBlock);


            String epicsBlock = epics.values().stream()
                    .sorted(Comparator.comparingInt(Task::getId))
                    .map(this::toString)
                    .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
            bufferedWriter.write(epicsBlock);


            String subtasksBlock = subtasks.values().stream()
                    .sorted(Comparator.comparingInt(Task::getId))
                    .map(this::toString)
                    .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
            bufferedWriter.write(subtasksBlock);

        } catch (IOException e) {
            throw new ManagerSaveException("Не удалось сохранить в файл: " + file, e);
        }
    }


    private String toString(Task task) {
        String epicCol = "";
        String durationCol = (task.getDuration() == null)
                ? ""
                : String.valueOf(task.getDuration().toMinutes());
        String startCol = (task.getStartTime() == null)
                ? ""
                : task.getStartTime().toString();

        return task.getId() + "," +
                TaskType.TASK + "," +
                task.getName() + "," +
                task.getStatus() + "," +
                task.getDescription() + "," +
                epicCol + "," +
                durationCol + "," +
                startCol;
    }


    private String toString(Epic epic) {

        return epic.getId() + "," +
                TaskType.EPIC + "," +
                epic.getName() + "," +
                epic.getStatus() + "," +
                epic.getDescription() + "," + "," + ",";
    }

    private String toString(Subtask subtask) {
        String durationCol = (subtask.getDuration() == null)
                ? ""
                : String.valueOf(subtask.getDuration().toMinutes());
        String startCol = (subtask.getStartTime() == null)
                ? ""
                : subtask.getStartTime().toString();
        return subtask.getId() + "," + TaskType.SUBTASK + "," + subtask.getName() + "," +
                subtask.getStatus() + "," + subtask.getDescription() + "," + subtask.getEpicId() + "," +
                durationCol + "," +
                startCol;
    }

    private Task fromString(String line) throws ManagerSaveException {
        String[] fields = line.split(",", -1);
        if (fields.length != 8) {
            throw new ManagerSaveException("CSV: ожидалось 8 колонок, получено " + fields.length + ": " + line);
        }

        TaskType type = TaskType.valueOf(fields[1]);

        int id = Integer.parseInt(fields[0]);
        String name = fields[2];

        String statusRaw = fields[3];
        TaskStatus status;
        if (statusRaw == null || statusRaw.isBlank() || "null".equalsIgnoreCase(statusRaw)) {
            status = TaskStatus.NEW;
        } else {
            status = TaskStatus.valueOf(statusRaw);
        }
        String description = fields[4];
        String epicStr = fields[5];
        String durStr = fields[6];
        String startStr = fields[7];

        Duration duration = durStr.isEmpty() ? null : Duration.ofMinutes(Long.parseLong(durStr));
        LocalDateTime startTime = startStr.isEmpty() ? null : LocalDateTime.parse(startStr);

        switch (type) {
            case TASK -> {
                Task task = new Task(name, description, status, duration, startTime);
                task.setId(id);
                return task;
            }
            case EPIC -> {
                Epic e = new Epic(name, description);
                e.setStatus(status);
                e.setId(id);
                return e;
            }
            case SUBTASK -> {
                if (epicStr.isEmpty())
                    throw new ManagerSaveException("CSV: у сабтаска пустой epicId: " + line);
                int epicId = Integer.parseInt(epicStr);
                Subtask subtask = new Subtask(name, description, status, epicId, duration, startTime);
                subtask.setId(id);
                return subtask;
            }
            default -> throw new ManagerSaveException("Unknown type: " + type);
        }
    }

}
