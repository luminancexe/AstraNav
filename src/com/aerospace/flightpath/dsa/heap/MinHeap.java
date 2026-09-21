package com.aerospace.flightpath.dsa.heap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Custom Generic Binary Min-Heap implementation from first principles.
 * Satisfies the Min-Heap Invariant: For every node i, heap[parent(i)] <= heap[i].
 *
 * Time Complexities:
 * - Insert (push): O(log N)
 * - Extract-Min (pop): O(log N)
 * - Peek: O(1)
 * - Heapify: O(N)
 */
public class MinHeap<T> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    @SuppressWarnings("unchecked")
    private T[] elements = (T[]) new Object[DEFAULT_INITIAL_CAPACITY];
    private int size = 0;
    private final Comparator<? super T> comparator;

    public MinHeap() {
        this(null);
    }

    public MinHeap(Comparator<? super T> comparator) {
        this.comparator = comparator;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Inserts a new element into the heap. O(log N)
     */
    public void insert(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot insert null into MinHeap");
        }
        ensureCapacity();
        elements[size] = item;
        siftUp(size);
        size++;
    }

    /**
     * Retrieves and removes the smallest element (root of the heap). O(log N)
     */
    public T extractMin() {
        if (isEmpty()) {
            throw new NoSuchElementException("MinHeap is empty");
        }
        T min = elements[0];
        elements[0] = elements[size - 1];
        elements[size - 1] = null;
        size--;

        if (size > 0) {
            siftDown(0);
        }
        return min;
    }

    /**
     * Returns the smallest element without removing it. O(1)
     */
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("MinHeap is empty");
        }
        return elements[0];
    }

    public void clear() {
        Arrays.fill(elements, 0, size, null);
        size = 0;
    }

    /**
     * Moves the element at index upward until the heap property is restored.
     */
    private void siftUp(int index) {
        int current = index;
        while (current > 0) {
            int parent = (current - 1) / 2;
            if (compare(elements[current], elements[parent]) < 0) {
                swap(current, parent);
                current = parent;
            } else {
                break;
            }
        }
    }

    /**
     * Moves the element at index downward until the heap property is restored.
     */
    private void siftDown(int index) {
        int current = index;
        while (true) {
            int leftChild = 2 * current + 1;
            int rightChild = 2 * current + 2;
            int smallest = current;

            if (leftChild < size && compare(elements[leftChild], elements[smallest]) < 0) {
                smallest = leftChild;
            }
            if (rightChild < size && compare(elements[rightChild], elements[smallest]) < 0) {
                smallest = rightChild;
            }

            if (smallest != current) {
                swap(current, smallest);
                current = smallest;
            } else {
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int compare(T a, T b) {
        if (comparator != null) {
            return comparator.compare(a, b);
        }
        return ((Comparable<? super T>) a).compareTo(b);
    }

    private void swap(int i, int j) {
        T temp = elements[i];
        elements[i] = elements[j];
        elements[j] = temp;
    }

    private void ensureCapacity() {
        if (size == elements.length) {
            elements = Arrays.copyOf(elements, elements.length * 2);
        }
    }
}
