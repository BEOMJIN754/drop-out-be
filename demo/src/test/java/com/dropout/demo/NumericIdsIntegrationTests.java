package com.dropout.demo;

import com.dropout.demo.domain.brand.entity.Brand;
import com.dropout.demo.domain.brand.repository.BrandRepository;
import com.dropout.demo.domain.release.repository.ReleaseRepository;
import com.dropout.demo.domain.release.service.SpaoCrawler;
import com.dropout.demo.domain.release.service.SpaoParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"supreme.crawler.enabled=false", "palace.crawler.enabled=false", "sansan.crawler.enabled=false", "spao.crawler.enabled=false", "LOGIN_SEED_USERNAME=", "LOGIN_SEED_PASSWORD="})
@AutoConfigureMockMvc
@Transactional
class NumericIdsIntegrationTests {
    @Autowired BrandRepository brands;
    @Autowired ReleaseRepository releases;
    @Autowired TransactionTemplate transaction;
    @Autowired MockMvc mvc;

    @Test void generatesIdsAndKeepsReleaseIdentityWhenRecrawled() throws Exception {
        var brand = brands.saveAndFlush(Brand.builder().name("Numeric test " + java.util.UUID.randomUUID()).build());
        assertTrue(brand.getId() > 0);
        mvc.perform(get("/api/brands/" + brand.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.slug").doesNotExist());
        String payload = """
            {"resultCode":"200","data":{"banner":[{"cornerNo":"R13701004_mainCollabo",
            "contentsBannerName":"202609_콜라보_숫자테스트_배너","contentsBannerTitleName":"",
            "subTitle2":"Before","image":[{"imageJoinUrl":"/p/planshop?exhibitionNo=999999999999&displayMallNo=0000137",
            "imageFilePathName":"r/image/display/2026-09-03/test.jpg"}]}]}}
            """;
        var crawler = new SpaoCrawler(new SpaoParser(), brands, releases, transaction);
        var item = new SpaoParser().parse(payload).getFirst();
        crawler.savePayload(payload);
        releases.flush();
        Long spao = brands.findByName("SPAO").orElseThrow().getId();
        var first = releases.findByBrandIdAndSourceUrlAndTitle(spao, item.sourceUrl(), item.title()).orElseThrow();
        Long id = first.getId();
        var createdAt = first.getCreatedAt();
        long count = releases.count();
        crawler.savePayload(payload.replace("Before", "After"));
        releases.flush();
        var updated = releases.findById(id).orElseThrow();
        assertEquals(count, releases.count());
        assertEquals(createdAt, updated.getCreatedAt());
        assertEquals("After", updated.getDescription());
        mvc.perform(get("/api/releases/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.brand.id").isNumber())
                .andExpect(jsonPath("$.slug").doesNotExist()).andExpect(jsonPath("$.piecesSummary").doesNotExist());
        mvc.perform(get("/api/releases").param("brand", brand.getId().toString()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
