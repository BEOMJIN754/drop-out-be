package com.dropout.demo.domain.user.service;

import com.dropout.demo.domain.user.entity.User;
import com.dropout.demo.domain.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Optional local account setup. Never overwrites an existing user's password. */
@Component
public class UserSeed implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final String username;
    private final String password;

    public UserSeed(
            UserRepository users,
            PasswordEncoder passwords,
            @Value("${LOGIN_SEED_USERNAME:}") String username,
            @Value("${LOGIN_SEED_PASSWORD:}") String password) {
        this.users = users;
        this.passwords = passwords;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isEmpty()) return;
        if (username.isBlank()
                || username.trim().length() > 50
                || password.isEmpty()
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException(
                    "Valid LOGIN_SEED_USERNAME and LOGIN_SEED_PASSWORD are required");
        if (users.findByUsername(username.trim()).isEmpty())
            users.save(new User(username.trim(), passwords.encode(password)));
    }
}
