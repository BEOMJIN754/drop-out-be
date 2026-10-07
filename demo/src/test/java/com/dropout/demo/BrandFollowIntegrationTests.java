package com.dropout.demo;

import com.dropout.demo.domain.brand.entity.Brand;
import com.dropout.demo.domain.brand.repository.BrandRepository;
import com.dropout.demo.domain.user.entity.User;
import com.dropout.demo.domain.user.repository.UserRepository;
import com.dropout.demo.domain.follow.BrandFollowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"supreme.crawler.enabled=false", "palace.crawler.enabled=false", "sansan.crawler.enabled=false", "spao.crawler.enabled=false", "LOGIN_SEED_USERNAME=", "LOGIN_SEED_PASSWORD="})
@AutoConfigureMockMvc
@Transactional
class BrandFollowIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BrandRepository brands;
    @Autowired BrandFollowRepository follows;

    @Test void persistsUniqueFollowsAndIsolatesAccounts() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        String alice = "a-" + suffix, bob = "b-" + suffix;
        var a = users.saveAndFlush(new User(alice, "test-hash"));
        users.saveAndFlush(new User(bob, "test-hash"));
        Long brand = brands.saveAndFlush(Brand.builder().name("Integration " + suffix).build()).getId();
        mvc.perform(get("/api/me/follows")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/follows/" + brand).with(user(alice))).andExpect(status().isForbidden());
        for (int i = 0; i < 2; i++) mvc.perform(put("/api/me/follows/" + brand).with(user(alice)).with(csrf())).andExpect(status().isNoContent());
        assertEquals(java.util.List.of(brand), follows.brandIds(a.getId()));
        mvc.perform(get("/api/me/follows").with(user(alice))).andExpect(jsonPath("$[0]").value(brand));
        mvc.perform(get("/api/me/follows").with(user(bob))).andExpect(content().json("[]"));
        mvc.perform(delete("/api/me/follows/" + brand).with(user(bob)).with(csrf())).andExpect(status().isNoContent());
        assertEquals(1, follows.brandIds(a.getId()).size());
        mvc.perform(put("/api/me/follows/9223372036854775807").with(user(alice)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/me/follows/" + brand).with(user(alice)).with(csrf())).andExpect(status().isNoContent());
        assertTrue(follows.brandIds(a.getId()).isEmpty());
    }

    @Test void requestsKeepUserForeignKeyAndDoNotLeakCredentials() throws Exception {
        String username = "r-" + java.util.UUID.randomUUID();
        users.saveAndFlush(new User(username, "secret-test-hash"));
        mvc.perform(post("/api/brand/requests").with(user(username)).with(csrf()).contentType("application/json")
                .content("{\"brandName\":\"Relation test\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.requestedBy").value(username)).andExpect(jsonPath("$.user").doesNotExist());
        mvc.perform(get("/api/brand/requests").with(user(username)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].brandName").value("Relation test"));
    }
}
