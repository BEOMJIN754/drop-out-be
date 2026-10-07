package com.dropout.demo.domain.user.service;

import com.dropout.demo.domain.user.entity.UserRole;
import com.dropout.demo.domain.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class LoginService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String dummyHash;

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(Long id, String username, UserRole role) {}

    public LoginService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null
                || request.username() == null
                || request.username().isBlank()
                || request.username().trim().length() > 50
                || request.password() == null
                || request.password().isEmpty()
                || request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디와 비밀번호를 확인해주세요.");
        }
        var user = users.findByUsername(request.username().trim());
        boolean matches =
                passwords.matches(
                        request.password(), user.map(u -> u.getPasswordHash()).orElse(dummyHash));
        if (user.isEmpty() || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return new LoginResponse(
                user.get().getId(), user.get().getUsername(), user.get().getRole());
    }
}
