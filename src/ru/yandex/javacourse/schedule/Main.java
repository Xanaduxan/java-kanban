package ru.yandex.javacourse.schedule;

import static ru.yandex.javacourse.schedule.tasks.TaskStatus.DONE;
import static ru.yandex.javacourse.schedule.tasks.TaskStatus.IN_PROGRESS;
import static ru.yandex.javacourse.schedule.tasks.TaskStatus.NEW;

import ru.yandex.javacourse.schedule.manager.Managers;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;

public class Main {
    public static void main(String[] args) {

        TaskManager manager = Managers.getDefault();

        // Создание
        Task task1 = new Task("Task #1", "Task1 description", NEW);
        Task task2 = new Task("Task #2", "Task2 description", IN_PROGRESS);
        final int taskId1 = manager.addNewTask(task1);
        final int taskId2 = manager.addNewTask(task2);

        Epic epic1 = new Epic("Epic #1", "Epic1 description");
        Epic epic2 = new Epic("Epic #2", "Epic2 description");
        final int epicId1 = manager.addNewEpic(epic1);
        final int epicId2 = manager.addNewEpic(epic2);

        Subtask subtask1 = new Subtask("Subtask #1-1", "Subtask1 description", NEW, epicId1);
        Subtask subtask2 = new Subtask("Subtask #2-1", "Subtask1 description", NEW, epicId1);
        Subtask subtask3 = new Subtask("Subtask #3-2", "Subtask1 description", DONE, epicId2);
        manager.addNewSubtask(subtask1);
        final Integer subtaskId2 = manager.addNewSubtask(subtask2);
        final Integer subtaskId3 = manager.addNewSubtask(subtask3);

        printAllTasks(manager);

        // Обновление
        final Task task = manager.getTask(taskId2);
        task.setStatus(DONE);
        manager.updateTask(task);
        System.out.println("CHANGE STATUS: Task2 IN_PROGRESS->DONE");
        System.out.println("Задачи:");
        for (Task t : manager.getTasks()) {
            System.out.println(t);
        }

        Subtask subtask = manager.getSubtask(subtaskId2);
        subtask.setStatus(DONE);
        manager.updateSubtask(subtask);
        System.out.println("CHANGE STATUS: Subtask2 NEW->DONE");
        subtask = manager.getSubtask(subtaskId3);
        subtask.setStatus(NEW);
        manager.updateSubtask(subtask);
        System.out.println("CHANGE STATUS: Subtask3 DONE->NEW");
        System.out.println("Подзадачи:");
        for (Task t : manager.getSubtasks()) {
            System.out.println(t);
        }

        System.out.println("Эпики:");
        for (Task e : manager.getEpics()) {
            System.out.println(e);
            for (Task t : manager.getEpicSubtasks(e.getId())) {
                System.out.println("--> " + t);
            }
        }
        final Epic epic = manager.getEpic(epicId1);
        epic.setStatus(NEW);
        manager.updateEpic(epic);
        System.out.println("CHANGE STATUS: Epic1 IN_PROGRESS->NEW");
        printAllTasks(manager);

        System.out.println("Эпики:");
        for (Task e : manager.getEpics()) {
            System.out.println(e);
            for (Task t : manager.getEpicSubtasks(e.getId())) {
                System.out.println("--> " + t);
            }
        }

        // Удаление
        System.out.println("DELETE: Task1");
        manager.deleteTask(taskId1);
        System.out.println("DELETE: Epic1");
        manager.deleteEpic(epicId1);
        printAllTasks(manager);

        // Доп. задание для проверки новой логики
//        int t1 = manager.addNewTask(new Task("Task #1", "Desc 1", NEW));
//        int t2 = manager.addNewTask(new Task("Task #2", "Desc 2", IN_PROGRESS));
//
//
//        int epicWithSubs = manager.addNewEpic(new Epic("Epic #A", "Epic with 3 subtasks"));
//        int s1 = manager.addNewSubtask(new Subtask("Subtask A-1", "s1", NEW, epicWithSubs));
//        int s2 = manager.addNewSubtask(new Subtask("Subtask A-2", "s2", NEW, epicWithSubs));
//        int s3 = manager.addNewSubtask(new Subtask("Subtask A-3", "s3", DONE, epicWithSubs));
//
//        int epicEmpty = manager.addNewEpic(new Epic("Epic #B (empty)", "No subtasks"));
//
//
//        manager.getTask(t1);
//        System.out.println("После getTask(t1)");
//        printAllTasks(manager);
//
//        manager.getEpic(epicWithSubs);
//        System.out.println("После getEpic(epicWithSubs)");
//        printAllTasks(manager);
//
//        manager.getSubtask(s1);
//        System.out.println("После getSubtask(s1)");
//        printAllTasks(manager);
//
//        manager.getTask(t2);
//        System.out.println("После getTask(t2)");
//        printAllTasks(manager);
//
//        manager.getSubtask(s3);
//        System.out.println("После getSubtask(s3)");
//        printAllTasks(manager);
//
//        manager.getEpic(epicEmpty);
//        System.out.println("После getEpic(epicEmpty)");
//        printAllTasks(manager);
//
//
//        manager.getTask(t1);
//        System.out.println("Повторный getTask(t1) — без дублей, t1 в конце");
//        printAllTasks(manager);
//
//        manager.getSubtask(s1);
//        System.out.println("Повторный getSubtask(s1) — без дублей, s1 в конце");
//        printAllTasks(manager);
//
//
//        manager.deleteTask(t2);
//        System.out.println("После deleteTask(t2) — t2 исчезает из истории");
//        printAllTasks(manager);
//
//
//        manager.getEpic(epicWithSubs);
//        manager.getSubtask(s2);
//        System.out.println("После дополнительных запросов epicWithSubs и s2");
//        printAllTasks(manager);
//
//
//        manager.deleteEpic(epicWithSubs);
//        System.out.println("После deleteEpic(epicWithSubs) — эпик A и его s1/s2/s3 исчезают");
//        printAllTasks(manager);
//
//
//        manager.getEpic(epicEmpty);
//        manager.getTask(t1);
//        System.out.println("Финал: история без удалённых, только живые элементы");
//        printAllTasks(manager);

    }

    private static void printAllTasks(TaskManager manager) {
        System.out.println("Задачи:");
        for (Task task : manager.getTasks()) {
            System.out.println(task);
        }
        System.out.println("Эпики:");
        for (Task epic : manager.getEpics()) {
            System.out.println(epic);
            System.out.println("--> Подзадачи эпика:");
            for (Task task : manager.getEpicSubtasks(epic.getId())) {
                System.out.println("--> " + task);
            }
        }
        System.out.println("Подзадачи:");
        for (Task subtask : manager.getSubtasks()) {
            System.out.println(subtask);
        }

        System.out.println("История:");
        for (Task task : manager.getHistory()) {
            System.out.println(task);
        }
    }
}
