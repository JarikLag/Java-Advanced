package ru.ifmo.rain.abubakirov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {
    private static final int FNV_32_PRIME = 0x01000193;
    private static final int FNV_32_START = 0x811c9dc5;
    private static final int FNV_BUFFER_SIZE = 1024;
    private static Path inputPath;
    private static Path outputPath;

    private static int calculateFNVHash(Path path) {
        int currentHash = FNV_32_START;
        try (InputStream reader = Files.newInputStream(path)) {
            int numRead;
            byte[] buff = new byte[FNV_BUFFER_SIZE];
            while ((numRead = reader.read(buff)) != -1) {
                for (int i = 0; i < numRead; ++i) {
                    currentHash *= FNV_32_PRIME;
                    currentHash ^= (buff[i] & 0xff);
                }
            }
        } catch (IOException e) {
            currentHash = 0;
        }
        return currentHash;
    }

    private static void walk() {
        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                String file;
                while ((file = reader.readLine()) != null) {
                    try {
                        Path path = Paths.get(file);
                        writer.write(String.format("%08x", calculateFNVHash(path)) + " " + file);
                    } catch (InvalidPathException e) {
                        writer.write(String.format("%08x", 0) + " " + file);
                    }
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("An error has occurred while working with output file");
            }
        } catch (IOException e) {
            System.err.println("An error has occurred while working with input file");
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
            System.err.println("Invalid arguments");
        } else {
            try {
                inputPath = Paths.get(args[0]);
            } catch (InvalidPathException e) {
                System.err.println("Invalid input file");
                return;
            }
            try {
                outputPath = Paths.get(args[1]);
                if (outputPath.getParent() != null) {
                    try {
                        Files.createDirectories(outputPath.getParent());
                    } catch (IOException e) {
                        System.err.println("Cannot create directories for output file");
                        return;
                    }
                }
            } catch (InvalidPathException e) {
                System.err.println("Invalid output file");
                return;
            }
            walk();
        }
    }
}
