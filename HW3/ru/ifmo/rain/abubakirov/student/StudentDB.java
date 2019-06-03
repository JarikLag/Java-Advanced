package ru.ifmo.rain.abubakirov.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {
    private static final Comparator<Student> nameComparator = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    private Stream<String> getMappedStream(List<Student> students, Function<Student, String> function) {
        return students
                .stream()
                .map(function);
    }

    private List<String> getMappedList(List<Student> students, Function<Student, String> function) {
        return getMappedStream(students, function)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getMappedList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getMappedList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return getMappedList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getMappedList(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getMappedStream(students, Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students
                .stream()
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse("");
    }

    private List<Student> getSortedList(Collection<Student> students, Comparator<Student> comparator) {
        return students
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return getSortedList(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return getSortedList(students, nameComparator);
    }

    private Stream<Student> getFilteredStream(Collection<Student> students, Predicate<Student> predicate) {
        return students
                .stream()
                .filter(predicate);
    }

    private List<Student> findStudentsBy(Collection<Student> students, Predicate<Student> predicate) {
        return getFilteredStream(students, predicate)
                .sorted(nameComparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, student -> student.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, student -> student.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return findStudentsBy(students, student -> student.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return getFilteredStream(students, student -> student.getGroup().equals(group))
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }
}
