package com.dropout.demo;

import com.dropout.demo.domain.user.entity.User;
import com.dropout.demo.domain.user.repository.UserRepository;
import com.dropout.demo.domain.user.service.LoginService;
import com.dropout.demo.domain.user.service.LoginService.LoginRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceTests {
    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final LoginService login = new LoginService(users, encoder);

    @Test void verifiesHashAndReturnsOnlyUserIdentity() {
        when(users.findByUsername("tester")).thenReturn(Optional.of(new User("tester", encoder.encode("correct-password"))));
        assertEquals("tester", login.login(new LoginRequest(" tester ", "correct-password")).username());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> login.login(new LoginRequest("tester", "wrong-password"))).getStatusCode().value());
    }

    @Test void unknownUserHasSameFailureAsWrongPassword() {
        when(users.findByUsername("missing")).thenReturn(Optional.empty());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> login.login(new LoginRequest("missing", "anything"))).getStatusCode().value());
    }

    @Test void rejectsMissingAndOversizedCredentials() {
        for (LoginRequest request : new LoginRequest[]{new LoginRequest(null, "pw"), new LoginRequest("", "pw"),
                new LoginRequest("tester", null), new LoginRequest("tester", ""), new LoginRequest("tester", "가".repeat(25))}) {
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> login.login(request)).getStatusCode().value());
        }
        verifyNoInteractions(users);
    }
}
