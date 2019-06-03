package ru.ifmo.abubakirov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;

import javax.accessibility.Accessible;
import javax.management.Descriptor;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Impler impler = new Implementor();
        Path path = Path.of("C:\\Users\\Yaroslav Dorokhov\\IdeaProjects\\HW4\\out\\production");
        try {
            impler.implement(Descriptor.class, path);
        } catch (Exception e) {
            System.out.println("Woopsie-doopsie! " + e.getMessage());
        }
    }
}