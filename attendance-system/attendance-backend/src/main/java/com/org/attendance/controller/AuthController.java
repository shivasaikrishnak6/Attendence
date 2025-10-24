package com.org.attendance.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class AuthController {

    @GetMapping("/auth/check")
    public Map<String, Object> check(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return Map.of(
                "username", auth.getName(),
                "admin", isAdmin
        );
    }
}

