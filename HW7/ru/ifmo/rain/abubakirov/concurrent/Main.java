package ru.ifmo.rain.abubakirov.concurrent;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        int PROCESSORS = Runtime.getRuntime().availableProcessors();
        System.out.println(PROCESSORS);
    }
}
