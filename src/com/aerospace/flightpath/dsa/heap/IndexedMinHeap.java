package com.aerospace.flightpath.dsa.heap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Indexed Binary Min-Heap mapping unique keys to comparable priorities.
 * Supports O(log N) decreaseKey and insertOrUpdate operations.
 *
 * Essential for textbook optimal Dijkstra's algorithm where node distances are updated in-place.
 */
public class IndexedMinHeap<K> {
    private static final int DEFAULT_CAPACITY = 16;

    private static class Entry<K> {
        final K key;
        double priority;

        Entry(K key, double priority) {
            this.key = key;
            this.priority = priority;
        }
    }

    @SuppressWarnings("unchecked")
    private Entry<K>[] heap = (Entry<K>[]) new Entry[DEFAULT_CAPACITY];
    private final Map<K, Integer> keyToIndex = new HashMap<>();
    private int size = 0;

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(K key) {
        return keyToIndex.containsKey(key);
    }

    public double getPriority(K key) {
        Integer idx = keyToIndex.get(key);
        if (idx == null) {
            throw new NoSuchElementException("Key not found in IndexedMinHeap: " + key);
        }
        return heap[idx].priority;
    }

    /**
     * Inserts key with initial priority, or decreases key if new priority is smaller. O(log N)
     */
    public void insertOrDecrease(K key, double priority) {
        Integer idx = keyToIndex.get(key);
        if (idx == null) {
            ensureCapacity();
            Entry<K> entry = new Entry<>(key, priority);
            heap[size] = entry;
            keyToIndex.put(key, size);
            siftUp(size);
            size++;
        } else if (priority < heap[idx].priority) {
            heap[idx].priority = priority;
            siftUp(idx);
        }
    }

    /**
     * Removes and returns the key with the minimum priority. O(log N)
     */
    public K extractMin() {
        if (isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }
        Entry<K> minEntry = heap[0];
        K minKey = minEntry.key;

        Entry<K> lastEntry = heap[size - 1];
        heap[0] = lastEntry;
        keyToIndex.put(lastEntry.key, 0);

        heap[size - 1] = null;
        keyToIndex.remove(minKey);
        size--;

        if (size > 0) {
            siftDown(0);
        }

        return minKey;
    }

    public void clear() {
        Arrays.fill(heap, 0, size, null);
        keyToIndex.clear();
        size = 0;
    }

    private void siftUp(int index) {
        int current = index;
        while (current > 0) {
            int parent = (current - 1) / 2;
            if (heap[current].priority < heap[parent].priority) {
                swap(current, parent);
                current = parent;
            } else {
                break;
            }
        }
    }

    private void siftDown(int index) {
        int current = index;
        while (true) {
            int left = 2 * current + 1;
            int right = 2 * current + 2;
            int smallest = current;

            if (left < size && heap[left].priority < heap[smallest].priority) {
                smallest = left;
            }
            if (right < size && heap[right].priority < heap[smallest].priority) {
                smallest = right;
            }

            if (smallest != current) {
                swap(current, smallest);
                current = smallest;
            } else {
                break;
            }
        }
    }

    private void swap(int i, int j) {
        Entry<K> temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;

        keyToIndex.put(heap[i].key, i);
        keyToIndex.put(heap[j].key, j);
    }

    private void ensureCapacity() {
        if (size == heap.length) {
            heap = Arrays.copyOf(heap, heap.length * 2);
        }
    }
}
