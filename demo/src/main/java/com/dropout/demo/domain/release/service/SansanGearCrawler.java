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
@ConditionalOnProperty(name = "sansan.crawler.enabled", havingValue = "true")
public class SansanGearCrawler implements ReleaseCrawler {
    private final SansanGearParser parser;
    private final BrandRepository brands;
    private final ReleaseRepository releases;
    private final TransactionTemplate transaction;
    public SansanGearCrawler(SansanGearParser parser, BrandRepository brands, ReleaseRepository releases, TransactionTemplate transaction) {
        this.parser = parser; this.brands = brands; this.releases = releases; this.transaction = transaction;
    }
    @Override
    public synchronized void crawl() {
        try {
            var request = HttpRequest.newBuilder(URI.create("https://sansangear.com/"))
                    .timeout(Duration.ofSeconds(30)).header("User-Agent", "DropoutMVP/1.0").GET().build();
            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 5_000_000)
                throw new IllegalStateException("Sansan Gear HTTP " + response.statusCode());
            var item = parser.parse(response.body());
            transaction.executeWithoutResult(status -> {
                Brand brand = brands.findByName("SAN SAN GEAR").orElseGet(() -> Brand.builder()
                        .name("SAN SAN GEAR")
                        .origin("SEOUL, KR").category(BrandCategory.TECHNICAL_STREETWEAR).officialUrl("https://sansangear.com/").build());
                brand.updateLogoUrl(item.logoUrl());
                brand = brands.save(brand);
                var previous = releases.findByBrandIdAndSourceUrlAndTitle(brand.getId(), "https://sansangear.com/", item.title());
                releases.save(Release.builder().id(previous.map(Release::getId).orElse(null)).brand(brand)
                        .title(item.title()).description(item.description())
                        .imageUrl(item.imageUrl()).releaseType(ReleaseType.SEASONAL)
                        .dateLabel("발매일 미확인")
                        
                        .sourceUrl("https://sansangear.com/")
                        .crawledAt(OffsetDateTime.now()).createdAt(previous.map(Release::getCreatedAt).orElse(null)).build());
            });
            LoggerFactory.getLogger(getClass()).info("Sansan Gear crawl saved {} and official logo", item.id());
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            LoggerFactory.getLogger(getClass()).warn("Sansan Gear crawl failed; retaining previous data", error);
        }
    }
}
