package ru.ifmo.abubakirov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Implementor implements Impler {
    private StringBuilder code;
    private Class currentClass;

    public Implementor() {

    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkArguments(token, root);
        Path path = createPathToFile(token, root);
        createDirectories(path);

        generateClass(token);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(code.toString());
        } catch (IOException e) {
            throw new ImplerException("I/O error while writing to class file");
        }
    }

    private void checkArguments(Class<?> token, Path path) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Not null token is required");
        } else if (path == null) {
            throw new ImplerException("Not null path is required");
        } else if (!token.isInterface()) {
            throw new ImplerException("Implementor supports only interfaces");
        }
    }

    private Path createPathToFile(Class<?> token, Path path) {
        return path.resolve(getPackageName(token).replace('.', File.separatorChar))
                .resolve(getImplementedClassName(token) + ".java");
    }

    private static String getPackageName(Class<?> token) {
        if (token.getPackage() == null)
            return "";
        return token.getPackage().getName();
    }

    private void createDirectories(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (SecurityException e) {
                throw new ImplerException("Not enough permissions for creating directories", e);
            } catch (IOException e) {
                throw new ImplerException("Cannot create directories for output class", e);
            }
        }
    }

    private static String getImplementedClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    private void generateClass(Class<?> token) {
        code = new StringBuilder();
        currentClass = token;

        addHeader();
        addBody();
        code.append("}");
    }

    private void addHeader() {
        if (!getPackageName(currentClass).equals("")) {
            code.append("package ")
                    .append(getPackageName(currentClass))
                    .append(";")
                    .append(System.lineSeparator());
        }
        code.append("public class ")
                .append(getImplementedClassName(currentClass))
                .append(" ")
                .append("implements ")
                .append(currentClass.getSimpleName())
                .append(" {")
                .append(System.lineSeparator());
    }

    private void addBody() {
        for (Method method : currentClass.getMethods()) {
            code.append(addMethodModifiersAndName(method))
                    .append("(")
                    .append(addMethodArguments(method))
                    .append(")")
                    .append(addMethodExceptions(method))
                    .append(" {")
                    .append(System.lineSeparator())
                    .append(addMethodBody(method))
                    .append(System.lineSeparator())
                    .append("}")
                    .append(System.lineSeparator());
        }
    }

    private StringBuilder addMethodModifiersAndName(Method method) {
        StringBuilder localBuilder = new StringBuilder();
        int methodModifiers = method.getModifiers() & (Modifier.methodModifiers() ^ Modifier.ABSTRACT);
        localBuilder.append(Modifier.toString(methodModifiers))
                .append(" ")
                .append(method.getReturnType().getCanonicalName())
                .append(" ")
                .append(method.getName());
        return localBuilder;
    }

    private StringBuilder addMethodArguments(Method method) {
        StringBuilder localBuilder = new StringBuilder();
        int argumentIndex = 0;
        for (Class argument : method.getParameterTypes()) {
            localBuilder.append(argument.getCanonicalName())
                    .append(" arg")
                    .append(argumentIndex);
            ++argumentIndex;
            if (argumentIndex != method.getParameterCount()) {
                localBuilder.append(", ");
            }
        }
        return localBuilder;
    }

    private StringBuilder addMethodExceptions(Method method) {
        StringBuilder localBuilder = new StringBuilder();
        int exceptionIndex = 0, exceptionsCount = method.getExceptionTypes().length;
        if (exceptionsCount != 0) {
            localBuilder.append(" throws ");
            for (Class exception : method.getExceptionTypes()) {
                localBuilder.append(exception.getCanonicalName());
                ++exceptionIndex;
                if (exceptionIndex != exceptionsCount) {
                    localBuilder.append(", ");
                }
            }
        }
        return localBuilder;
    }

    private StringBuilder addMethodBody(Method method) {
        StringBuilder localBuilder = new StringBuilder();
        Class returnType = method.getReturnType();
        if (returnType == void.class) {
            localBuilder.append("return;");
        } else if (returnType.isPrimitive()) {
            if (returnType == boolean.class) {
                localBuilder.append("return false;");
            } else {
                localBuilder.append("return 0;");
            }
        } else {
            localBuilder.append("return null;");
        }
        return localBuilder;
    }
}