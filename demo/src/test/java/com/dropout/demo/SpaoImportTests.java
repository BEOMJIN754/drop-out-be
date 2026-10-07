package com.dropout.demo;

import com.dropout.demo.domain.release.service.*;
import com.dropout.demo.domain.release.repository.ReleaseRepository;
import com.dropout.demo.domain.brand.repository.BrandRepository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in command for importing an official capture into the configured DB. */
@EnabledIfEnvironmentVariable(named = "SPAO_IMPORT", matches = "true")
@SpringBootTest(properties = {"spao.crawler.enabled=false", "supreme.crawler.enabled=false", "palace.crawler.enabled=false", "sansan.crawler.enabled=false", "LOGIN_SEED_USERNAME=", "LOGIN_SEED_PASSWORD="})
class SpaoImportTests {
    @Autowired SpaoParser parser;
    @Autowired BrandRepository brands;
    @Autowired ReleaseRepository releases;
    @Autowired TransactionTemplate transaction;
    @Test void importsAndUpdatesWithoutDuplicates() throws Exception {
        String payload = java.nio.file.Files.readString(java.nio.file.Path.of(System.getenv("SPAO_CAPTURE")));
        var crawler = new SpaoCrawler(parser, brands, releases, transaction);
        int count = crawler.savePayload(payload);
        long stored = releases.findByBrandId(brands.findByName("SPAO").orElseThrow().getId()).size();
        assertTrue(stored >= count);
        crawler.savePayload(payload);
        assertEquals(stored, releases.findByBrandId(brands.findByName("SPAO").orElseThrow().getId()).size());
        assertTrue(releases.findByBrandId(brands.findByName("SPAO").orElseThrow().getId()).stream().allMatch(r -> r.getReleaseAt() == null
                && r.getReleaseDate() == null));
        assertThrows(Exception.class, () -> crawler.savePayload("{}"));
        assertEquals(stored, releases.findByBrandId(brands.findByName("SPAO").orElseThrow().getId()).size());
    }
}
