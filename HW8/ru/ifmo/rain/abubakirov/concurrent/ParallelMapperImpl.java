package ru.ifmo.rain.abubakirov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> tasks;
    private final List<Thread> workers;
    private final static int MAX_SIZE = 1000000;

    public ParallelMapperImpl(int threads) {
        tasks = new ArrayDeque<>();
        workers = new ArrayList<>();
        for (int i = 0; i < threads; ++i) {
            Thread thread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        doTask();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
            workers.add(thread);
            thread.start();
        }
    }

    private void doTask() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notifyAll();
        }
        task.run();
    }

    private void addTask(final Runnable task) throws InterruptedException {
        synchronized (tasks) {
            while (tasks.size() == MAX_SIZE) {
                tasks.wait();
            }
            tasks.add(task);
            tasks.notifyAll();
        }
    }

    private class ConcurrentCollector<R> {
        private final List<R> result;
        private int count;

        ConcurrentCollector(int size) {
            result = new ArrayList<>(Collections.nCopies(size, null));
            count = 0;
        }

        void setData(int pos, R data) {
            result.set(pos, data);
            synchronized (this) {
                if (++count == result.size()) {
                    notify();
                }
            }
        }

        synchronized List<R> getResult() throws InterruptedException {
            while (count < result.size()) {
                wait();
            }
            return result;
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> args) throws InterruptedException {
        ConcurrentCollector<R> collector = new ConcurrentCollector<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int pos = i;
            addTask(() -> collector.setData(pos, function.apply(args.get(pos))));
        }
        return collector.getResult();
    }

    @Override
    public void close() {
        workers.forEach(Thread::interrupt);
        for (Thread thread : workers) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}