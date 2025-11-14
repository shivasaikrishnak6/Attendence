package com.org.attendance.controller;

import com.org.attendance.model.Attendance;
import com.org.attendance.model.EmployeeSessionView;
import com.org.attendance.repository.AttendanceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    private final AttendanceRepository attendanceRepository;

    public AdminAuthController(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    // Used by admin.html guard to ensure user is ADMIN
    @GetMapping("/check")
    public Map<String, Object> authCheck(Authentication auth) {
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        return Map.of(
                "username", auth != null ? auth.getName() : null,
                "admin", isAdmin
        );
    }

    // 1️⃣ Active Users – employees who signed in today and haven’t signed out yet
    @GetMapping("/active-users")
    public List<EmployeeSessionView> activeUsers() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        List<Attendance> activeToday =
                attendanceRepository.findBySignOutTimeIsNullAndSignInTimeBetween(start, end);

        return activeToday.stream()
                .map(EmployeeSessionView::from)
                .toList();
    }

    // 2️⃣ Login History – all attendance records, latest login first
    @GetMapping("/all-sessions")
    public List<EmployeeSessionView> allSessions() {
        return attendanceRepository.findAllByOrderBySignInTimeDesc()
                .stream()
                .map(EmployeeSessionView::from)
                .toList();
    }
}

