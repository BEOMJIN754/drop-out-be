package com.dropout.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dropout.demo.domain.user.entity.User;
import com.dropout.demo.domain.user.repository.UserRepository;
import com.dropout.demo.domain.user.service.CurrentUserService;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

class CurrentUserServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUserService currentUser = new CurrentUserService(users);

    @Test
    void missingPrincipalIsUnauthorizedWithoutQueryingDatabase() {
        var error =
                assertThrows(ResponseStatusException.class, () -> currentUser.requireUser(null));
        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(users);
    }

    @Test
    void deletedAccountIsUnauthorized() {
        when(users.findByUsername("deleted")).thenReturn(Optional.empty());
        var error =
                assertThrows(
                        ResponseStatusException.class,
                        () -> currentUser.requireUser(() -> "deleted"));
        assertEquals(401, error.getStatusCode().value());
    }

    @Test
    void resolvesAuthenticatedAccount() {
        var user = new User("alice", "unused-hash");
        when(users.findByUsername("alice")).thenReturn(Optional.of(user));
        assertSame(user, currentUser.requireUser(() -> "alice"));
    }
}
