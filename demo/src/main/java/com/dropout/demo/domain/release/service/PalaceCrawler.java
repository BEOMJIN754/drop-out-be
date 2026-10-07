package com.dropout.demo.domain.release.service;

import com.dropout.demo.domain.brand.entity.Brand;
import com.dropout.demo.domain.brand.entity.BrandCategory;
import com.dropout.demo.domain.brand.repository.BrandRepository;
import com.dropout.demo.domain.release.entity.*;
import com.dropout.demo.domain.release.repository.ReleaseRepository;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.ArrayList;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnProperty(name = "palace.crawler.enabled", havingValue = "true")
public class PalaceCrawler implements ReleaseCrawler {
    private static final String ORIGIN = "https://palaceskateboards.com";
    private final PalaceParser parser;
    private final BrandRepository brands;
    private final ReleaseRepository releases;
    private final TransactionTemplate transaction;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public PalaceCrawler(PalaceParser parser, BrandRepository brands, ReleaseRepository releases, TransactionTemplate transaction) {
        this.parser = parser; this.brands = brands; this.releases = releases; this.transaction = transaction;
    }
    private String fetch(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(ORIGIN + path)).timeout(Duration.ofSeconds(30))
                .header("User-Agent", "DropoutMVP/1.0").GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().length() > 5_000_000)
            throw new IllegalStateException("Palace HTTP " + response.statusCode());
        return response.body();
    }
    @Override
    public synchronized void crawl() {
        var log = LoggerFactory.getLogger(getClass());
        try {
            String home = fetch("/");
            int year = parser.seasonYear(home);
            var items = new ArrayList<PalaceParser.Item>();
            for (String path : parser.links(home)) {
                try { items.add(parser.parse(fetch(path), path, year)); }
                catch (InterruptedException error) { throw error; }
                catch (Exception error) { log.warn("Palace skipped {}: {}", path, error.getMessage()); }
            }
            if (items.isEmpty()) throw new IllegalStateException("No Palace launch announcements parsed");
            transaction.executeWithoutResult(status -> {
                Brand brand = brands.findByName("Palace").orElseGet(() -> brands.save(Brand.builder()
                        .name("Palace").origin("LONDON, UK").foundedYear(2009)
                        .category(BrandCategory.SKATE_STREETWEAR).officialUrl(ORIGIN).build()));
                for (var item : items) {
                    var previous = releases.findByBrandIdAndSourceUrlAndTitle(brand.getId(), ORIGIN + "/advice/" + item.slug(), item.title());
                    releases.save(Release.builder().id(previous.map(Release::getId).orElse(null)).brand(brand)
                            .title(item.title()).description(item.description())
                            .imageUrl(item.imageUrl()).releaseType(ReleaseType.COLLAB).releaseAt(item.releaseAt())
                            .releaseDate(item.releaseAt() == null ? null : item.releaseAt().toLocalDate())
                            .dateLabel("KR in-store; year inferred from active season")
                            
                            
                            .sourceUrl(ORIGIN + "/advice/" + item.slug())
                            .crawledAt(OffsetDateTime.now()).createdAt(previous.map(Release::getCreatedAt).orElse(null)).build());
                }
            });
            log.info("Palace crawl saved {} releases", items.size());
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Palace crawl failed; retaining previous data", error);
        }
    }
}
