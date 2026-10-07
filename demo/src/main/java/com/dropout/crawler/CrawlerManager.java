package com.dropout.crawler;

import com.dropout.demo.domain.release.service.ReleaseCrawler;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import org.slf4j.LoggerFactory;

public class CrawlerManager {
    private final List<ReleaseCrawler> crawlers;

    public CrawlerManager(List<ReleaseCrawler> crawlers) {
        this.crawlers = List.copyOf(crawlers);
    }

    /** Run every enabled crawler once, concurrently, and wait for completion. */
    public synchronized void start() {
        var log = LoggerFactory.getLogger(getClass());
        log.info("Starting {} enabled crawlers", crawlers.size());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = crawlers.stream()
                    .map(crawler -> CompletableFuture.runAsync(crawler::crawl, executor))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(tasks).join();
        }
        log.info("Crawler run finished; check individual crawler logs for results");
    }
}
