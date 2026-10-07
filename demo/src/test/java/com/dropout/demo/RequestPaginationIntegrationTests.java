package com.dropout.demo;

import com.dropout.demo.domain.request.*;
import com.dropout.demo.domain.user.entity.User;
import com.dropout.demo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"supreme.crawler.enabled=false", "palace.crawler.enabled=false", "sansan.crawler.enabled=false", "spao.crawler.enabled=false", "LOGIN_SEED_USERNAME=", "LOGIN_SEED_PASSWORD="})
@AutoConfigureMockMvc
@Transactional
class RequestPaginationIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired BrandRequestRepository requests;

    @Test void returnsTwentyThenOneAndExcludesOtherUsers() throws Exception {
        String name = "page-" + java.util.UUID.randomUUID();
        var owner = users.saveAndFlush(new User(name, "test-hash"));
        var other = users.saveAndFlush(new User("other-" + java.util.UUID.randomUUID(), "test-hash"));
        for (int i = 0; i < 21; i++) requests.save(new BrandRequest("Brand " + i, "", "", owner));
        requests.saveAndFlush(new BrandRequest("Other user's brand", "", "", other));
        mvc.perform(get("/api/brand/requests").with(user(name)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.items[0].brandName").value("Brand 20"))
                .andExpect(jsonPath("$.items[19].brandName").value("Brand 1"))
                .andExpect(jsonPath("$.items[0].requestedBy").value(name))
                .andExpect(jsonPath("$.items[0].user").doesNotExist())
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/brand/requests?page=1").with(user(name)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].brandName").value("Brand 0"))
                .andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/api/brand/requests?page=2").with(user(name)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
        mvc.perform(get("/api/brand/requests?page=-1").with(user(name))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/brand/admin/requests?page=0").with(user(name).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.hasNext").value(true));
    }
}
