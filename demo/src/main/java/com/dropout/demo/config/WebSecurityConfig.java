package com.dropout.demo.config;

import com.dropout.demo.domain.user.entity.UserRole;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String[] PUBLIC_URLS = {"/api/auth/csrf", "/error"};

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.ignoringRequestMatchers(LOGIN_URL))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.POST, LOGIN_URL)
                                        .permitAll()
                                        .requestMatchers(PUBLIC_URLS)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**", "/api/brand/admin/**")
                                        .hasRole(UserRole.ADMIN.name())
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/releases/**",
                                                "/api/brands/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                        (request, response, error) -> response.sendError(401)))
                .logout(
                        logout ->
                                logout.logoutUrl("/api/auth/logout")
                                        .logoutSuccessHandler(
                                                (request, response, auth) ->
                                                        response.setStatus(204)))
                .build();
    }
}
