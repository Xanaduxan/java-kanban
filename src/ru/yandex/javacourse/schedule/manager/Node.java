package ru.yandex.javacourse.schedule.manager;

public final class Node<E> {
    private final E data;
    private Node<E> next;
    private Node<E> prev;

    public Node(Node<E> prev, E data, Node<E> next) {
        this.prev = prev;
        this.data = data;
        this.next = next;
    }

    public E getData() {
        return data;
    }

    public Node<E> getNext() {
        return this.next;
    }

    public Node<E> getPrev() {
        return this.prev;
    }

    public void setPrev(Node<E> prev) {
        this.prev = prev;
    }

    public void setNext(Node<E> next) {
        this.next = next;
    }

}

