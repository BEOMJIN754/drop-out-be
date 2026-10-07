package com.dropout.crawler;

import com.dropout.demo.domain.brand.entity.Brand;
import com.dropout.demo.domain.brand.repository.BrandRepository;
import com.dropout.demo.domain.release.entity.Release;
import com.dropout.demo.domain.release.repository.ReleaseRepository;
import com.dropout.demo.domain.release.service.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Standalone, one-shot crawler entry point; does not start the API server. */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {Brand.class, Release.class})
@EnableJpaRepositories(basePackageClasses = {BrandRepository.class, ReleaseRepository.class})
@Import({CrawlerManager.class,
        SupremeCrawler.class, SupremeParser.class,
        PalaceCrawler.class, PalaceParser.class,
        SansanGearCrawler.class, SansanGearParser.class,
        SpaoCrawler.class, SpaoParser.class})
public class CrawlerApplication {
    public static void main(String[] args) {
        try (var context = new SpringApplicationBuilder(CrawlerApplication.class)
                .web(WebApplicationType.NONE).run(args)) {
            context.getBean(CrawlerManager.class).start();
        }
    }
}
