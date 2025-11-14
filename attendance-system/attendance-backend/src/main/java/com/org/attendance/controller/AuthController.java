package com.org.attendance.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // Called from index.html to validate username/password
    @GetMapping("/check")
    public Map<String, Object> check(Authentication auth) {
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        String username = (auth != null) ? auth.getName() : null;

        return Map.of(
                "username", username,
                "admin", isAdmin
        );
    }

    // Called from user.html / admin.html on Logout – we don’t need
    // to do anything server-side, but keep endpoint so frontend works.
    @PostMapping("/logout")
    public void logout() {
        // no-op: front-end just clears localStorage and redirects
    }
}

