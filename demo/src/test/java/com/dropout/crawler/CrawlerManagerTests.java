package com.dropout.crawler;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.dropout.demo.domain.release.service.ReleaseCrawler;
import static org.junit.jupiter.api.Assertions.*;

class CrawlerManagerTests {
    @Test
    void startsCrawlersConcurrentlyAndWaitsForAll() {
        var started = new CountDownLatch(4);
        var finished = new AtomicInteger();
        ReleaseCrawler crawler = () -> {
            started.countDown();
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS), "Crawlers must run concurrently");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError(error);
            }
            finished.incrementAndGet();
        };
        new CrawlerManager(List.of(crawler, crawler, crawler, crawler)).start();
        assertEquals(4, finished.get());
    }

    @Test
    void allowsAllCrawlersToBeDisabled() {
        assertDoesNotThrow(() -> new CrawlerManager(List.of()).start());
    }
}
