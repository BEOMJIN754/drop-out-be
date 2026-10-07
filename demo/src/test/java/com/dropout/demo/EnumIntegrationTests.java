package com.dropout.demo;

import com.dropout.demo.domain.user.entity.*;
import com.dropout.demo.domain.user.repository.UserRepository;
import com.dropout.demo.domain.brand.entity.BrandCategory;
import com.dropout.demo.domain.release.entity.ReleaseStatus;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties = {"supreme.crawler.enabled=false", "palace.crawler.enabled=false", "sansan.crawler.enabled=false", "spao.crawler.enabled=false", "LOGIN_SEED_USERNAME=", "LOGIN_SEED_PASSWORD="})
@AutoConfigureMockMvc
@Transactional
class EnumIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test void loginAuthoritiesComeFromPersistedRole() throws Exception {
        for (var role : UserRole.values()) {
            String name = "role-" + java.util.UUID.randomUUID();
            var account = new User(name, passwords.encode("test-password"));
            account.changeRole(role);
            users.saveAndFlush(account);
            var result = mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content("{\"username\":\"" + name + "\",\"password\":\"test-password\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role.name()))
                    .andExpect(jsonPath("$.admin").value(role == UserRole.ADMIN)).andReturn();
            var session = (MockHttpSession) result.getRequest().getSession(false);
            mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.role").value(role.name()));
            mvc.perform(get("/api/brand/admin/requests").session(session))
                    .andExpect(role == UserRole.ADMIN ? status().isOk() : status().isForbidden());
        }
        assertEquals(UserRole.USER, new User("default", "hash").getRole());
    }

    @Test void acceptsEnumCategoryAndRejectsUnknownValues() throws Exception {
        mvc.perform(post("/api/brands/create").with(user("tester")).with(csrf()).contentType("application/json")
                .content("{\"name\":\"Enum " + java.util.UUID.randomUUID() + "\",\"category\":\"SPORTSWEAR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.category").value(BrandCategory.SPORTSWEAR.name()));
        mvc.perform(post("/api/brands/create").with(user("tester")).with(csrf()).contentType("application/json")
                .content("{\"name\":\"Bad\",\"category\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/releases").with(user("tester")).with(csrf()).contentType("application/json")
                .content("{\"brandId\":1,\"releaseType\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/brand/admin/requests/1/status").with(user("tester").roles("ADMIN")).with(csrf()).contentType("application/json")
                .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void releaseStatusRespectsInstantAndKoreanDateBoundaries() {
        var clock = Clock.fixed(Instant.parse("2026-09-10T15:00:00Z"), ZoneOffset.UTC);
        assertEquals(ReleaseStatus.UNKNOWN, ReleaseStatus.from(null, null, clock));
        assertEquals(ReleaseStatus.RELEASED, ReleaseStatus.from(null, LocalDate.of(2026, 9, 10), clock));
        assertEquals(ReleaseStatus.UPCOMING, ReleaseStatus.from(null, LocalDate.of(2026, 9, 11), clock));
        assertEquals(ReleaseStatus.RELEASED, ReleaseStatus.from(OffsetDateTime.now(clock), null, clock));
        assertEquals(ReleaseStatus.UPCOMING, ReleaseStatus.from(OffsetDateTime.now(clock).plusSeconds(1), null, clock));
    }
}
