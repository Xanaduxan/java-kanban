package ru.yandex.javacourse.schedule.manager;

import java.util.*;

import ru.yandex.javacourse.schedule.tasks.Task;

/**
 * In memory history manager.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public class InMemoryHistoryManager implements HistoryManager {
    private final Map<Integer, Node<Task>> history = new HashMap<>();

    private Node<Task> head;
    private Node<Task> tail;
    private int size = 0;

    @Override
    public List<Task> getHistory() {
        List<Task> result = new ArrayList<>(size);
        Node<Task> currentHead = head;
        while (currentHead != null) {
            result.add(currentHead.data);
            currentHead = currentHead.next;
        }
        return result;
    }

    @Override
    public void add(Task task) {
        if (task == null) {
            return;
        }
        Node<Task> existing = history.get(task.getId());
        if (existing != null) {
            removeNode(existing);
        }
        addNewNode(task);

    }

    public void addNewNode(Task task) {
        Node<Task> newNode = new Node<>(tail, task, null);
        if (tail != null) {
            tail.next = newNode;
        } else {
            head = newNode;
        }
        tail = newNode;
        history.put(task.getId(), newNode);
        size++;

    }

    public void removeNode(Node<Task> node) {
        if (node == null) return;
        Node<Task> prev = node.prev;
        Node<Task> next = node.next;

        if (prev != null) {
            prev.next = next;
        } else {
            head = next;
        }
        if (next != null) {
            next.prev = prev;
        } else {
            tail = prev;
        }
        Task taskToRemove = node.data;
        if (taskToRemove != null) {
            history.remove(taskToRemove.getId());
        }
        size--;
    }

    @Override
    public void remove(int id) {
        Node<Task> node = history.get(id);
        if (node != null) {
            removeNode(node);
        }

    }

    private static class Node<E> {
        E data;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E data, Node<E> next) {
            this.prev = prev;
            this.data = data;
            this.next = next;
        }
    }
}
