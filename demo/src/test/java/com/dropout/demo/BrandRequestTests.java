package com.dropout.demo;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dropout.demo.config.WebSecurityConfig;
import com.dropout.demo.domain.request.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BrandRequestController.class)
@Import({
    WebSecurityConfig.class,
    BrandRequestService.class,
    com.dropout.demo.domain.user.service.CurrentUserService.class
})
class BrandRequestTests {
    @Test
    void adminCanReviewButMembersCannot() throws Exception {
        var alice = new com.dropout.demo.domain.user.entity.User("alice", "secret");
        var request = new BrandRequest("Example", "", "", alice);
        when(requests.findById(1L)).thenReturn(java.util.Optional.of(request));
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String path = "/api/brand/admin/requests/1/status";
        mvc.perform(
                        patch(path)
                                .with(user("alice"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        patch(path)
                                .with(user("admin").roles("ADMIN"))
                                .contentType("application/json")
                                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
        for (String state : new String[] {"APPROVED", "REJECTED"})
            mvc.perform(
                            patch(path)
                                    .with(user("admin").roles("ADMIN"))
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content("{\"status\":\"" + state + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(state));
        mvc.perform(
                        patch(path)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        patch("/api/brand/admin/requests/999/status")
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isNotFound());
    }

    @Autowired MockMvc mvc;
    @MockitoBean BrandRequestRepository requests;
    @MockitoBean com.dropout.demo.domain.user.repository.UserRepository users;

    @org.junit.jupiter.api.BeforeEach
    void setupUser() {
        var alice = new com.dropout.demo.domain.user.entity.User("alice", "never-exposed");
        org.springframework.test.util.ReflectionTestUtils.setField(alice, "id", 42L);
        when(users.findByUsername("alice")).thenReturn(java.util.Optional.of(alice));
    }

    @Test
    void requiresLoginAndAdminRole() throws Exception {
        mvc.perform(get("/api/brand/requests")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/brand/admin/requests").with(user("alice")))
                .andExpect(status().isForbidden());
        when(requests.findAll(any(Pageable.class))).thenReturn(Page.empty());
        mvc.perform(get("/api/brand/admin/requests").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void requiresCsrfAndUsesAuthenticatedAuthor() throws Exception {
        String body =
                "{\"brandName\":\" Example"
                    + " \",\"officialUrl\":\"https://example.com\",\"reason\":\"Please"
                    + " add\",\"requestedBy\":\"admin\"}";
        mvc.perform(
                        post("/api/brand/requests")
                                .with(user("alice"))
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isForbidden());
        when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(
                        post("/api/brand/requests")
                                .with(user("alice"))
                                .with(csrf())
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestedBy").value("alice"))
                .andExpect(jsonPath("$.brandName").value("Example"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsUnsafeUrlAndBlankName() throws Exception {
        for (String body :
                new String[] {
                    "{\"brandName\":\" \"}",
                    "{\"brandName\":\"Example\",\"officialUrl\":\"javascript:alert(1)\"}"
                }) {
            mvc.perform(
                            post("/api/brand/requests")
                                    .with(user("alice"))
                                    .with(csrf())
                                    .contentType("application/json")
                                    .content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(requests);
    }

    @Test
    void listsOnlyOwnRequests() throws Exception {
        when(requests.findByUser_IdOrderByCreatedAtDescIdDesc(eq(42L), any()))
                .thenReturn(Page.empty());
        mvc.perform(get("/api/brand/requests").with(user("alice"))).andExpect(status().isOk());
        verify(requests).findByUser_IdOrderByCreatedAtDescIdDesc(eq(42L), any());
    }
}
