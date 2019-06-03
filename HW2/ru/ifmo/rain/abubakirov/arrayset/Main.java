package ru.ifmo.rain.abubakirov.arrayset;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        List<Integer> list = List.of(1567665002, 1292170604, -1249582105, -1026883331, -471144123, 1904682106, 2135459, 698855823, 1670186295, 437571160);
        SortedSet<Integer> set = new ArraySet<>(list, Comparator.comparingInt(i -> 0));
        TreeSet<Integer> treeSet = new TreeSet<>(Comparator.comparingInt(i -> 0));
        treeSet.addAll(list);
        System.out.println("Array Set:\n");
        for (final Integer element : list) {
            System.out.println(set.contains(element));
        }
        System.out.println("Tree Set:\n");
        for (final Integer element : list) {
            System.out.println(treeSet.contains(element));
        }
    }
}
