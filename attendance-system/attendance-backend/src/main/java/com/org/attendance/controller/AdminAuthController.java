package com.org.attendance.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    // ✅ Check login role (used in index.html for redirect)
    @GetMapping("/check")
    public Map<String, Object> check(Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return Map.of(
                "username", auth.getName(),
                "admin", isAdmin
        );
    }

    // ✅ Return dummy active users
    @GetMapping("/active-users")
    public List<Map<String, Object>> activeUsers() {
        return List.of(
                Map.of("id", 1, "username", "user1", "loginTime", LocalDateTime.now().minusMinutes(10).toString()),
                Map.of("id", 2, "username", "user2", "loginTime", LocalDateTime.now().minusMinutes(20).toString())
        );
    }

    // ✅ Return dummy login history
    @GetMapping("/all-sessions")
    public List<Map<String, Object>> allSessions() {
        return List.of(
                Map.of("id", 1, "username", "admin", "loginTime", LocalDateTime.now().minusHours(1).toString(), "logoutTime", "", "active", true),
                Map.of("id", 2, "username", "user", "loginTime", LocalDateTime.now().minusHours(3).toString(), "logoutTime", LocalDateTime.now().minusHours(2).toString(), "active", false)
        );
    }

    // ✅ Dummy endpoint for deleting employees
    @DeleteMapping("/employees/{id}")
    public Map<String, Object> deleteEmployee(@PathVariable int id) {
        return Map.of("deletedId", id, "status", "ok");
    }
}

