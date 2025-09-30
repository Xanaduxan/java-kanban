package ru.yandex.javacourse.schedule.manager;

final class Node<E> {
    private final E data;
    private Node<E> next;
    private Node<E> prev;

    Node(Node<E> prev, E data, Node<E> next) {
        this.prev = prev;
        this.data = data;
        this.next = next;
    }

    E getData() {
        return data;
    }

    Node<E> getNext() {
        return this.next;
    }

    Node<E> getPrev() {
        return this.prev;
    }

    void setPrev(Node<E> prev) {
        this.prev = prev;
    }

    void setNext(Node<E> next) {
        this.next = next;
    }

}

