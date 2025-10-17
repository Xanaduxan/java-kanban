package ru.yandex.javacourse.schedule.manager;

import ru.yandex.javacourse.schedule.tasks.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class FileBackedTaskManager extends InMemoryTaskManager {

    private final Path file;
    private static final String HEADER = "id,type,name,status,description,epic";

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

            for (Subtask s : buffer) {
                Epic e = manager.epics.get(s.getEpicId());
                if (e == null) {
                    throw new ManagerSaveException("Нет эпика " + s.getEpicId() + " для сабтаска " + s.getId());
                }
                manager.subtasks.put(s.getId(), s);
                e.addSubtaskId(s.getId());
            }

            for (Epic e : manager.epics.values()) {
                manager.updateEpicStatus(e.getId());
            }

            manager.generatorId = maxId;
        } catch (IOException e) {
            throw new ManagerSaveException("Не удалось прочитать файл: " + path, e);
        }

        return manager;
    }



    private void save() {
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            bufferedWriter.write(HEADER);
            bufferedWriter.write(System.lineSeparator());


            List<Task> taskList = new ArrayList<>(tasks.values());
            taskList.sort(Comparator.comparingInt(Task::getId));
            for (Task t : taskList) {
                bufferedWriter.write(toString(t));
                bufferedWriter.write(System.lineSeparator());
            }

            List<Epic> epicList = new ArrayList<>(epics.values());
            epicList.sort(Comparator.comparingInt(Task::getId));
            for (Epic e : epicList) {
                bufferedWriter.write(toString(e));
                bufferedWriter.write(System.lineSeparator());
            }

            List<Subtask> subtaskList = new ArrayList<>(subtasks.values());
            subtaskList.sort(Comparator.comparingInt(Task::getId));
            for (Subtask s : subtaskList) {
                bufferedWriter.write(toString(s));
                bufferedWriter.write(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Не удалось сохранить в файл: " + file, e);
        }
    }


    private String toString(Task task) {
        return task.getId() + "," + TaskType.TASK + "," + task.getName() + "," +
                task.getStatus() + "," + task.getDescription() + ",";
    }

    private String toString(Epic epic) {
        return epic.getId() + "," + TaskType.EPIC + "," + epic.getName() + "," +
                epic.getStatus() + "," + epic.getDescription() + ",";
    }

    private String toString(Subtask subtask) {
        return subtask.getId() + "," + TaskType.SUBTASK + "," + subtask.getName() + "," +
                subtask.getStatus() + "," + subtask.getDescription() + "," + subtask.getEpicId();
    }

    private Task fromString(String line) throws ManagerSaveException {
        String[] fields = line.split(",", -1);
        if (fields.length != 6) {
            throw new ManagerSaveException("CSV: ожидалось 6 колонок, получено " + fields.length + ": " + line);
        }

        TaskType type = TaskType.valueOf(fields[1]);

        int id = Integer.parseInt(fields[0]);
        String name = fields[2];
        TaskStatus status = TaskStatus.valueOf(fields[3]);
        String description = fields[4];

        switch (type) {
            case TASK -> {
                return new Task(id,
                        name,
                        description,
                        status);
            }
            case EPIC -> {
                Epic e = new Epic(name, description);
                e.setStatus(status);
                e.setId(id);
                return e;
            }
            case SUBTASK -> {
                if (fields[5].isEmpty())
                    throw new ManagerSaveException("CSV: у сабтаска пустой epicId: " + line);
                int epicId = Integer.parseInt(fields[5]);
                return new Subtask(id,
                        name,
                        description, status, epicId
                );
            }
            default -> throw new ManagerSaveException("Unknown type: " + type);
        }
    }

}
