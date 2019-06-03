package ru.ifmo.rain.abubakirov;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final ConcurrentMap<String, HostData> hosts;

    private class HostData {
        final Queue<Runnable> tasks;
        int counter;

        HostData() {
            tasks = new ArrayDeque<>();
            counter = 0;
        }

        private synchronized void addTask(Runnable task) {
            if (counter < perHost) {
                ++counter;
                downloadersPool.submit(task);
            } else {
                tasks.add(task);
            }
        }

        private synchronized void nextTask() {
            final Runnable next = tasks.poll();
            if (next != null) {
                downloadersPool.submit(next);
            } else {
                --counter;
            }
        }
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);
        hosts = new ConcurrentHashMap<>();
    }

    public WebCrawler(int downloaders, int extractors, int perHost) throws IOException {
        this(new CachingDownloader(), downloaders, extractors, perHost);
    }

    private void extractLinks(final Document page, final int depth, final Set<String> downloaded,
                              final ConcurrentMap<String, IOException> errors, final Phaser phaser,
                              final Set<String> used) {
        try {
            page.extractLinks().stream().filter(used::add)
                    .forEach(link -> downloadPage(link, depth, downloaded, errors, phaser, used));
        } catch (IOException ignored) {
        } finally {
            phaser.arrive();
        }
    }

    private void downloadPage(final String url, final int depth, final Set<String> downloaded,
                          final ConcurrentMap<String, IOException> errors, final Phaser phaser,
                          final Set<String> used) {
        try {
            final String host = URLUtils.getHost(url);
            final HostData data = hosts.computeIfAbsent(host, s -> new HostData());
            phaser.register();
            data.addTask(() -> {
                try {
                    final Document page = downloader.download(url);
                    downloaded.add(url);
                    if (depth > 1) {
                        phaser.register();
                        extractorsPool.submit(() -> extractLinks(page, depth - 1, downloaded, errors, phaser, used));
                    }
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arrive();
                    data.nextTask();
                }
            });
        } catch (MalformedURLException e) {
            errors.put(url, e);
        }
    }

    @Override
    public Result download(String url, int depth) {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        final Set<String> used = ConcurrentHashMap.newKeySet();
        Phaser phaser = new Phaser(1);
        used.add(url);
        downloadPage(url, depth, downloaded, errors, phaser, used);
        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(downloaded), errors);
    }

    @Override
    public void close() {
        downloadersPool.shutdownNow();
        extractorsPool.shutdownNow();
    }

    private static int getArgument(String[] args, int index, int defaultValue) throws NumberFormatException {
        if (args.length > index) {
            return Integer.parseInt(args[index]);
        }
        else {
            return defaultValue;
        }
    }

    public static void main(String[] args) {
        final int defaultDownloadersNumber = Runtime.getRuntime().availableProcessors();
        final int defaultExtractorsNumber = Runtime.getRuntime().availableProcessors();
        final int defaultPerHost = 20;
        if (args == null || args.length < 2 || args.length > 5) {
            System.err.println("Wrong number of arguments");
            return;
        }
        int[] arguments = new int[4];
        try {
            arguments[0] = getArgument(args, 1, 1);
            arguments[1] = getArgument(args, 2, defaultDownloadersNumber);
            arguments[2] = getArgument(args, 3, defaultExtractorsNumber);
            arguments[3] = getArgument(args, 4, defaultPerHost);
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments");
            return;
        }
        try (WebCrawler crawler = new WebCrawler(arguments[1], arguments[2], arguments[3])) {
            crawler.download(args[0], arguments[0]);
        } catch (IOException e) {
            System.err.println("Can't create WebCrawler");
            return;
        }
    }
}
