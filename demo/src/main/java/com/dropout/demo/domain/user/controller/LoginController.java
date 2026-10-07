package com.dropout.demo.domain.user.controller;

import com.dropout.demo.domain.user.entity.UserRole;
import com.dropout.demo.domain.user.service.LoginService;
import com.dropout.demo.domain.user.service.LoginService.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
    private final LoginService login;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public LoginController(LoginService login) {
        this.login = login;
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        var user = login.login(request);
        boolean admin = user.role() == UserRole.ADMIN;
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.username(),
                        null,
                        List.of(new SimpleGrantedAuthority(user.role().authority()))));
        if (servletRequest.getSession(false) != null) servletRequest.changeSessionId();
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return Map.of(
                "id", user.id(), "username", user.username(), "role", user.role(), "admin", admin);
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        UserRole role =
                auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals(UserRole.ADMIN.authority()))
                        ? UserRole.ADMIN
                        : UserRole.USER;
        return Map.of("username", auth.getName(), "role", role, "admin", role == UserRole.ADMIN);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> invalidLogin(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(Map.of("message", error.getReason()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> invalidBody() {
        return ResponseEntity.badRequest().body(Map.of("message", "아이디와 비밀번호를 확인해주세요."));
    }
}
