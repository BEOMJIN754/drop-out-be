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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnProperty(name = "supreme.crawler.enabled", havingValue = "true")
public class SupremeCrawler implements ReleaseCrawler {
    private final SupremeParser parser;
    private final BrandRepository brands;
    private final ReleaseRepository releases;
    private final TransactionTemplate transaction;
    private final String dataset;
    public SupremeCrawler(SupremeParser parser, BrandRepository brands, ReleaseRepository releases,
                          TransactionTemplate transaction, @Value("${supreme.crawler.dataset}") String dataset) {
        this.parser = parser; this.brands = brands; this.releases = releases;
        this.transaction = transaction; this.dataset = dataset;
    }

    @Override
    public synchronized void crawl() {
        try {
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            var request = HttpRequest.newBuilder(URI.create("https://supreme.com/news"))
                    .timeout(Duration.ofSeconds(30)).header("User-Agent", "DropoutMVP/1.0").GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body().length() > 5_000_000)
                throw new IllegalStateException("Supreme HTTP " + response.statusCode());
            var items = parser.parse(response.body());
            transaction.executeWithoutResult(status -> {
                Brand brand = brands.findByName("Supreme").orElseGet(() -> brands.save(Brand.builder()
                        .name("Supreme").origin("NEW YORK, US")
                        .foundedYear(1994).category(BrandCategory.STREETWEAR).officialUrl("https://supreme.com").build()));
                for (var item : items) {
                    var existing = releases.findByBrandIdAndSourceUrlAndTitle(brand.getId(), "https://supreme.com/news/" + item.slug(), item.title());
                    String image = null;
                    if (item.imageAsset().matches("image-[a-zA-Z0-9]+-\\d+x\\d+-[a-z]+")) {
                        String asset = item.imageAsset().substring(6);
                        int dot = asset.lastIndexOf('-');
                        image = "https://cdn.sanity.io/images/ldn4d4qt/" + dataset + "/"
                                + asset.substring(0, dot) + "." + asset.substring(dot + 1) + "?w=1200&auto=format";
                    }
                    releases.save(Release.builder().id(existing.map(Release::getId).orElse(null)).brand(brand)
                            .title(item.title()).description(item.description())
                            .releaseType(ReleaseType.DROP).releaseDate(item.releaseDate())
                            
                            .dateLabel(item.releaseDate() == null ? "Asia release date TBA" : "Asia release")
                            .imageUrl(image)
                            .sourceUrl("https://supreme.com/news/" + item.slug())
                            .crawledAt(OffsetDateTime.now())
                            .createdAt(existing.map(Release::getCreatedAt).orElse(null)).build());
                }
            });
            LoggerFactory.getLogger(getClass()).info("Supreme crawl saved {} news releases", items.size());
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            LoggerFactory.getLogger(getClass()).warn("Supreme crawl failed; retaining previous data", error);
        }
    }
}
