package com.dropout.demo.domain.release.service;

import com.dropout.demo.domain.brand.entity.Brand;
import com.dropout.demo.domain.brand.entity.BrandCategory;
import com.dropout.demo.domain.brand.repository.BrandRepository;
import com.dropout.demo.domain.release.entity.*;
import com.dropout.demo.domain.release.repository.ReleaseRepository;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnProperty(name = "spao.crawler.enabled", havingValue = "true")
public class SpaoCrawler implements ReleaseCrawler {
    public static final String SOURCE = "https://www.spao.com/v1/corner/banners/primary?cornerNo=R13701004_mainCollabo";
    private final SpaoParser parser;
    private final BrandRepository brands;
    private final ReleaseRepository releases;
    private final TransactionTemplate transaction;
    public SpaoCrawler(SpaoParser parser, BrandRepository brands, ReleaseRepository releases, TransactionTemplate transaction) {
        this.parser = parser; this.brands = brands; this.releases = releases; this.transaction = transaction;
    }
    @Override
    public synchronized void crawl() {
        try {
            var request = HttpRequest.newBuilder(URI.create(SOURCE)).timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "DropoutMVP/1.0").GET().build();
            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 5_000_000)
                throw new IllegalStateException("SPAO HTTP " + response.statusCode());
            int count = savePayload(response.body());
            LoggerFactory.getLogger(getClass()).info("SPAO crawl saved {} collaborations", count);
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            LoggerFactory.getLogger(getClass()).warn("SPAO crawl failed; retaining previous data", error);
        }
    }
    public int savePayload(String payload) throws Exception {
        var items = parser.parse(payload);
        transaction.executeWithoutResult(status -> {
            var brand = brands.findByName("SPAO").orElseGet(() -> brands.save(Brand.builder()
                    .name("SPAO").origin("SEOUL, KR").category(BrandCategory.CASUAL_COLLABORATION)
                    .officialUrl("https://www.spao.com/").build()));
            for (var item : items) {
                var previous = releases.findByBrandIdAndSourceUrlAndTitle(brand.getId(), item.sourceUrl(), item.title());
                releases.save(Release.builder().id(previous.map(Release::getId).orElse(null)).brand(brand)
                        .title(item.title()).description(item.description()).imageUrl(item.imageUrl())
                        .releaseType(ReleaseType.COLLAB).releaseDate(null)
                        
                        .dateLabel("공식 콜라보 기획전 · 발매일 미확인")
                        .sourceUrl(item.sourceUrl())
                        .crawledAt(OffsetDateTime.now()).createdAt(previous.map(Release::getCreatedAt).orElse(null)).build());
            }
        });
        return items.size();
    }
}
