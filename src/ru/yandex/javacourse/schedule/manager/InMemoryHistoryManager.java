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
            result.add(currentHead.getData());
            currentHead = currentHead.getNext();
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

    private void addNewNode(Task task) {
        Node<Task> newNode = new Node<>(tail, task, null);
        if (tail != null) {
            tail.setNext(newNode);
        } else {
            head = newNode;
        }
        tail = newNode;
        history.put(task.getId(), newNode);
        size++;

    }

    private void removeNode(Node<Task> node) {
        if (node == null) return;
        Node<Task> prev = node.getPrev();
        Node<Task> next = node.getNext();

        if (prev != null) {
            prev.setNext(next);
        } else {
            head = next;
        }
        if (next != null) {
            next.setPrev(prev);
        } else {
            tail = prev;
        }
        Task taskToRemove = node.getData();
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


}
