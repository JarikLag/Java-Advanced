package ru.ifmo.rain.abubakirov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {
    private ParallelMapper parallelMapper;

    /**
     * Default constructor.
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    /**
     * Constructor from custom parallelMapper
     * @param parallelMapper
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T, R> R doTask(int threadsNumber, List<? extends T> values,
                            Function<? super Stream<? extends T>, ? extends R> task,
                            Function<? super Stream<? extends R>, ? extends R> collector)
            throws InterruptedException {
        threadsNumber = Math.max(1, Math.min(values.size(), threadsNumber));
        List<Stream<? extends T>> concurrentTasks = new ArrayList<>();
        int sizeOfBlock = values.size() / threadsNumber;
        int remaining = values.size() % threadsNumber;

        for (int l, r = 0, i = 0; i < threadsNumber; ++i) {
            l = r;
            r = l + sizeOfBlock;
            if (remaining > 0) {
                ++r;
                --remaining;
            }
            concurrentTasks.add(values.subList(l, r).stream());
        }

        List<R> result;
        if (parallelMapper != null) {
            result = parallelMapper.map(task, concurrentTasks);
        } else {
            List<Thread> workers = new ArrayList<>();
            result = new ArrayList<>(Collections.nCopies(threadsNumber, null));
            for (int i = 0; i < threadsNumber; ++i) {
                final int pos = i;
                Thread thread = new Thread(() -> result.set(pos, task.apply(concurrentTasks.get(pos))));
                workers.add(thread);
                thread.start();
            }
            InterruptedException exception = null;
            for (Thread thread : workers) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    if (exception == null) {
                        exception = new InterruptedException();
                    }
                    exception.addSuppressed(e);
                }
            }
            if (exception != null) {
                throw exception;
            }
        }
        return collector.apply(result.stream());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        Function<? super Stream<? extends T>, ? extends T> maxStream = stream -> stream.max(comparator).orElse(null);
        return doTask(threads, values, maxStream, maxStream);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        Function<? super Stream<? extends T>, ? extends T> minStream = stream -> stream.min(comparator).orElse(null);
        return doTask(threads, values, minStream, minStream);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return doTask(threads, values, stream -> stream.allMatch(predicate), stream -> stream.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return doTask(threads, values, stream -> stream.anyMatch(predicate), stream -> stream.anyMatch(Boolean::booleanValue));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return doTask(threads, values, stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> function) throws InterruptedException {
        return doTask(threads, values, stream -> stream.map(function).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return doTask(threads, values, stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }
}
