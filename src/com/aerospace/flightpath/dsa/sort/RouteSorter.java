package com.aerospace.flightpath.dsa.sort;

import com.aerospace.flightpath.model.RouteResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Custom implementations of fundamental Divide-and-Conquer Sorting Algorithms (QuickSort & MergeSort)
 * used for ranking alternative flight routes and sorting navigational waypoints.
 */
public class RouteSorter {

    /**
     * In-place QuickSort with median-of-three pivot selection.
     * Time Complexity: Average O(N log N), Worst O(N^2). Space: O(log N) stack.
     */
    public static <T> void quickSort(List<T> list, Comparator<? super T> comp) {
        if (list == null || list.size() <= 1) return;
        quickSortHelper(list, 0, list.size() - 1, comp);
    }

    private static <T> void quickSortHelper(List<T> list, int low, int high, Comparator<? super T> comp) {
        if (low < high) {
            int pivotIndex = partition(list, low, high, comp);
            quickSortHelper(list, low, pivotIndex - 1, comp);
            quickSortHelper(list, pivotIndex + 1, high, comp);
        }
    }

    private static <T> int partition(List<T> list, int low, int high, Comparator<? super T> comp) {
        // Median-of-three pivot selection
        int mid = low + (high - low) / 2;
        if (comp.compare(list.get(mid), list.get(low)) < 0) swap(list, low, mid);
        if (comp.compare(list.get(high), list.get(low)) < 0) swap(list, low, high);
        if (comp.compare(list.get(high), list.get(mid)) < 0) swap(list, mid, high);

        T pivot = list.get(high);
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (comp.compare(list.get(j), pivot) <= 0) {
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    /**
     * Stable MergeSort implementation.
     * Time Complexity: Guaranteed O(N log N). Space: O(N).
     */
    public static <T> List<T> mergeSort(List<T> list, Comparator<? super T> comp) {
        if (list == null || list.size() <= 1) {
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        }
        return mergeSortHelper(new ArrayList<>(list), comp);
    }

    private static <T> List<T> mergeSortHelper(List<T> list, Comparator<? super T> comp) {
        if (list.size() <= 1) return list;

        int mid = list.size() / 2;
        List<T> left = mergeSortHelper(new ArrayList<>(list.subList(0, mid)), comp);
        List<T> right = mergeSortHelper(new ArrayList<>(list.subList(mid, list.size())), comp);

        return merge(left, right, comp);
    }

    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<? super T> comp) {
        List<T> merged = new ArrayList<>(left.size() + right.size());
        int i = 0, j = 0;

        while (i < left.size() && j < right.size()) {
            if (comp.compare(left.get(i), right.get(j)) <= 0) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }

        while (i < left.size()) merged.add(left.get(i++));
        while (j < right.size()) merged.add(right.get(j++));

        return merged;
    }

    private static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    /**
     * Predefined Comparators for ranking flight route alternatives.
     */
    public static final Comparator<RouteResult> BY_DISTANCE =
            Comparator.comparingDouble(RouteResult::getTotalDistanceKm);

    public static final Comparator<RouteResult> BY_TIME =
            Comparator.comparingDouble(RouteResult::getTotalTimeHours);

    public static final Comparator<RouteResult> BY_FUEL =
            Comparator.comparingDouble(RouteResult::getTotalFuelKg);

    public static final Comparator<RouteResult> BY_HOPS =
            Comparator.comparingInt(RouteResult::getHopCount);
}
