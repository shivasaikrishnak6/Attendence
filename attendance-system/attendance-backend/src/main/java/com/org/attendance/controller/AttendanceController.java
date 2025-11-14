package com.org.attendance.controller;

import com.org.attendance.model.Attendance;
import com.org.attendance.model.Employee;
import com.org.attendance.repository.AttendanceRepository;
import com.org.attendance.repository.EmployeeRepository;
import com.org.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceService attendanceService;

    private static final DateTimeFormatter DISPLAY_TS_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy, hh:mm:ss a");

    // ==============================
    // Helpers
    // ==============================

    // Helper: extract employeeCode from body (accepts "employeeCode" or "employeeId")
    private String extractEmployeeCode(Map<String, String> body) {
        String code = null;
        if (body != null) {
            code = body.get("employeeCode");
            if (code == null || code.isBlank()) {
                // frontend currently sends "employeeId", but it's actually the code
                code = body.get("employeeId");
            }
        }
        return (code == null) ? null : code.trim();
    }

    // ==============================
    // Basic endpoints
    // ==============================

    // 🧾   Get all raw attendance rows (mostly for debugging)
    @GetMapping
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    // 🟢   Sign In  (JSON: { "employeeId": "BCG5656" } or { "employeeCode": "BCG5656" })
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody Map<String, String> request) {
        String employeeCode = extractEmployeeCode(request);
        if (employeeCode == null || employeeCode.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "⚠️ Employee ID is required"));
        }

        Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(employeeCode);
        if (empOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "❌ Employee not found for code: " + employeeCode));
        }

        // Check if they already have an open session (signed in but not signed out)
        Optional<Attendance> latestOpt = attendanceService.findLatestByEmployeeCode(employeeCode);
        if (latestOpt.isPresent() && latestOpt.get().getSignOutTime() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "⚠️ Already signed in!"));
        }

        Employee emp = empOpt.get();

        Attendance attendance = new Attendance();
        attendance.setEmployee(emp);
        attendance.setSignInTime(LocalDateTime.now());
        attendance.setSignOutTime(null);

        attendanceRepository.save(attendance);

        return ResponseEntity.ok(Map.of(
                "message", "✅ Sign-In recorded successfully",
                "employeeCode", employeeCode,
                "timestamp", DISPLAY_TS_FMT.format(attendance.getSignInTime())
        ));
    }

    // 🔴   Sign Out (JSON: { "employeeId": "BCG5656" } or { "employeeCode": "BCG5656" })
    @PostMapping("/signout")
    public ResponseEntity<?> signOut(@RequestBody Map<String, String> request) {
        String employeeCode = extractEmployeeCode(request);
        if (employeeCode == null || employeeCode.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "⚠️ Employee ID is required"));
        }

        // Latest attendance row for this employeeCode
        Optional<Attendance> attendanceOpt =
                attendanceService.findLatestByEmployeeCode(employeeCode);

        if (attendanceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "⚠️ No active session found for this employee!"));
        }

        Attendance latest = attendanceOpt.get();
        if (latest.getSignOutTime() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "⚠️ Already signed out!"));
        }

        latest.setSignOutTime(LocalDateTime.now());
        attendanceRepository.save(latest);

        return ResponseEntity.ok(Map.of(
                "message", "✅ Sign-Out recorded successfully",
                "employeeCode", employeeCode,
                "timestamp", DISPLAY_TS_FMT.format(latest.getSignOutTime())
        ));
    }

    // 📊  Active users today (for Admin "Active Users" panel)
    @GetMapping("/active-today")
    public List<Map<String, Object>> getActiveToday() {
        return attendanceService.getTodayActiveUsers()
                .stream()
                .map(a -> Map.<String, Object>of(
                        "employeeCode", a.getEmployee().getEmployeeCode(),
                        "name", a.getEmployee().getName(),
                        "signInTime", DISPLAY_TS_FMT.format(a.getSignInTime())
                ))
                .collect(Collectors.toList());
    }

    // 📜  Login history (supports optional filter by employeeCode)
    @GetMapping("/history")
    public List<Map<String, Object>> getHistory(
            @RequestParam(name = "employeeCode", required = false) String employeeCode
    ) {
        List<Attendance> list;

        if (employeeCode != null && !employeeCode.isBlank()) {
            list = attendanceService.getLoginHistoryForEmployee(employeeCode.trim());
        } else {
            list = attendanceService.getLoginHistory();
        }

        return list.stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "employeeCode", a.getEmployee().getEmployeeCode(),
                        "name", a.getEmployee().getName(),
                        "signInTime", a.getSignInTime() != null ? DISPLAY_TS_FMT.format(a.getSignInTime()) : "",
                        "signOutTime", a.getSignOutTime() != null ? DISPLAY_TS_FMT.format(a.getSignOutTime()) : "",
                        "active", a.getSignOutTime() == null
                ))
                .collect(Collectors.toList());
    }

    // ==============================
    // CSV Export: /attendance/export
    // ==============================
    // Example:
    //   /attendance/export?year=2025&month=11              -> all employees for Nov 2025
    //   /attendance/export?year=2025&month=11&employeeCode=E000001 -> only that employee
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAttendanceCsv(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(name = "employeeCode", required = false) String employeeCode
    ) {
        YearMonth ym;
        try {
            ym = YearMonth.of(year, month);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(("Invalid year/month").getBytes(StandardCharsets.UTF_8));
        }

        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(LocalTime.MAX);

        List<Attendance> rows;
        if (employeeCode != null && !employeeCode.isBlank()) {
            rows = attendanceRepository.findByEmployeeEmployeeCodeAndSignInTimeBetween(
                    employeeCode.trim(), start, end
            );
        } else {
            rows = attendanceRepository.findBySignInTimeBetween(start, end);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Employee Code,Name,Sign In,Sign Out\n");

        for (Attendance a : rows) {
            String code = Optional.ofNullable(a.getEmployee().getEmployeeCode()).orElse("");
            String name = Optional.ofNullable(a.getEmployee().getName()).orElse("");
            String signIn = a.getSignInTime() != null ? DISPLAY_TS_FMT.format(a.getSignInTime()) : "";
            String signOut = a.getSignOutTime() != null ? DISPLAY_TS_FMT.format(a.getSignOutTime()) : "";

            sb.append(escapeCsv(code)).append(',')
              .append(escapeCsv(name)).append(',')
              .append(escapeCsv(signIn)).append(',')
              .append(escapeCsv(signOut)).append('\n');
        }

        String monthLabel = ym.getMonth().toString(); // e.g. NOVEMBER
        String safeMonth = monthLabel.substring(0, 3).toUpperCase(); // e.g. NOV
        String suffix = (employeeCode != null && !employeeCode.isBlank())
                ? "-" + employeeCode.trim()
                : "-ALL";
        String filename = "attendance-" + safeMonth + "-" + year + suffix + ".csv";

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}

