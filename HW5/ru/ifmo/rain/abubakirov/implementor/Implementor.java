package ru.ifmo.rain.abubakirov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Implementation class for {@link JarImpler} interface.
 */
public class Implementor implements JarImpler {
    /**
     * {@link StringBuilder} variable for saving string representation of code.
     */
    private StringBuilder code;
    /**
     * Current {@link Class}, which we are implementing.
     */

    private Class currentClass;

    /**
     * {@link SimpleFileVisitor} used for recursive deleting of folders.
     */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Default constructor.
     */
    public Implementor() {

    }

    /**
     * @throws ImplerException given class can't be generated for one of such reasons:
     *                         <ul>
     *                         <li> Some arguments are null or can't implement </li>
     *                         <li> Can't create files or directories. </li>
     *                         <li> {@link JavaCompiler} failed </li>
     *                         <li> The problems with I/O occurred during implementation. </li>
     *                         </ul>
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        checkArguments(token, jarFile);
        createDirectories(jarFile);

        Path tmpDir;
        try {
            tmpDir = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Can't create temporary directory", e);
        }

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            implement(token, tmpDir);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            String[] args = new String[]{
                    "-cp",
                    tmpDir.toString() + File.pathSeparator + System.getProperty("java.class.path"),
                    getPathToFile(token, tmpDir, ".java").toString()
            };

            if (compiler.run(null, null, null, args) != 0) {
                throw new ImplerException("Can't compile generated file");
            }
            writer.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
            Files.copy(getPathToFile(token, tmpDir, ".class"), writer);
        } catch (IOException e) {
            throw new ImplerException("Can't create .jar file", e);
        } finally {
            try {
                clean(tmpDir);
            } catch (IOException e) {
                System.err.println("Can't delete temporary directories " + e.getMessage());
            }
        }
    }

    /**
     * @throws ImplerException given class cannot be generated for one of such reasons:
     *                         <ul>
     *                         <li> Some arguments are null or can't implement</li>
     *                         <li> Can't create files or directories. </li>
     *                         <li> The problems with I/O occurred during implementation. </li>
     *                         <li> Class isn't an Interface </li>
     *                         </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        checkArguments(token, root);
        Path path = getPathToFile(token, root, ".java");
        createDirectories(path);

        generateClass(token);

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(toUnicode(code.toString()));
        } catch (IOException e) {
            throw new ImplerException("I/O error while writing to class file");
        }
    }

    /**
     * Clean all including files and directories in input path.
     *
     * @param root input path.
     * @throws IOException if occurs error while walking.
     */
    private static void clean(final Path root) throws IOException {
        if (Files.exists(root)) {
            Files.walkFileTree(root, DELETE_VISITOR);
        }
    }

    /**
     * Convert to unicode string
     *
     * @param in input string
     * @return converted string
     */
    private String toUnicode(String in) {
        StringBuilder b = new StringBuilder();
        for (char c : in.toCharArray()) {
            if (c >= 128) {
                b.append("\\u").append(String.format("%04X", (int) c));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Check input arguments for correctness.
     *
     * @param token input class.
     * @param path  input path.
     * @throws ImplerException if arguments is null or we can't implement this objects.
     */
    private void checkArguments(Class<?> token, Path path) throws ImplerException {
        if (token == null) {
            throw new ImplerException("Not null token is required");
        } else if (path == null) {
            throw new ImplerException("Not null path is required");
        } else if (!token.isInterface()) {
            throw new ImplerException("Implementor supports only interfaces");
        }
    }

    /**
     * Get full path to implementing class
     *
     * @param token input class.
     * @param path  input path.
     * @param end   suffix for path.
     * @return {@link Path} with full path to class.
     */
    private Path getPathToFile(Class<?> token, Path path, String end) {
        return path.resolve(getPackageName(token).replace('.', File.separatorChar))
                .resolve(getImplementedClassName(token) + end);
    }

    /**
     * Get class package in right format.
     *
     * @param token {@link Class} for which we want get package.
     * @return Name returned by {@link Package#getName()} function for package got by {@link Class#getPackage()} function.
     */
    private static String getPackageName(Class<?> token) {
        if (token.getPackage() == null)
            return "";
        return token.getPackage().getName();
    }

    /**
     * Create directory for files.
     *
     * @param path path for creating.
     * @throws ImplerException if {@link IOException} or {@link SecurityException}
     *                         occurs in {@link Files#createDirectories(Path, FileAttribute[])} function.
     */
    private void createDirectories(Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (SecurityException e) {
                throw new ImplerException("Not enough permissions for creating directories", e);
            } catch (IOException e) {
                throw new ImplerException("Can't create directories for output class", e);
            }
        }
    }

    /**
     * Get implementation class name in right format.
     *
     * @param token {@link Class} for which we want get name.
     * @return Name returned by {@link Class#getSimpleName()} function with "Impl" suffix.
     */
    private static String getImplementedClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Generate implementation of class. Result will be stored in global {@link StringBuilder}.
     *
     * @param token class for implementing
     */
    private void generateClass(Class<?> token) {
        code = new StringBuilder();
        currentClass = token;

        addHeader();
        addBody();
        code.append("}");
    }

    /**
     * Generate header of class: package and name of class. Result will be stored in global {@link StringBuilder}.
     */
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
                .append(currentClass.getCanonicalName())
                .append(" {")
                .append(System.lineSeparator());
    }

    /**
     * Generate body of class: all methods and constructors. Result will be stored in global {@link StringBuilder}.
     */
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

    /**
     * Generate correct modifiers and name for {@link Method}.
     *
     * @param method method to be processed.
     * @return {@link StringBuilder} with string representation of method modifiers and name.
     */
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

    /**
     * Generate correct list of arguments for {@link Method}.
     *
     * @param method method to be processed.
     * @return {@link StringBuilder} with string representation of list of arguments.
     */
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

    /**
     * Generate list of {@link Exception}s for {@link Method}.
     *
     * @param method method to be processed.
     * @return {@link StringBuilder} with string representation of list of exceptions.
     */
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

    /**
     * Generate body for {@link Method}.
     *
     * @param method method to be processed.
     * @return {@link StringBuilder} with string representation of method body.
     */
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

    /**
     * Start point for program
     *
     * @param args [ "class_name", "output_path" ] if java-file required
     *             [ "-jar", "class_name", "output_path" ] if jar-file required
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.err.println("Two or three arguments required");
            return;
        }
        for (String s : args) {
            if (s == null) {
                System.err.println("Not null arguments required");
                return;
            }
        }
        JarImpler impler = new Implementor();
        try {
            if (args.length == 2) {
                impler.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                impler.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ImplerException e) {
            System.err.println("Error during implementation " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Class for implementing not found " + e.getMessage());
        }
    }
}