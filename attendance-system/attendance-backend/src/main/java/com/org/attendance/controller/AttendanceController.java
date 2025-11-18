package com.org.attendance.controller;

import com.org.attendance.model.Attendance;
import com.org.attendance.model.Employee;
import com.org.attendance.repository.AttendanceRepository;
import com.org.attendance.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

    private static final ZoneId APP_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter TABLE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");
    private static final DateTimeFormatter CSV_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // ───────────────────────────────── Helper methods ─────────────────────────────

    /** Accepts "employeeCode" or legacy "employeeId" from JSON body. */
    private String extractEmployeeCode(Map<String, String> body) {
        if (body == null) return null;
        String code = body.get("employeeCode");
        if (code == null || code.isBlank()) {
            code = body.get("employeeId");
        }
        return (code == null) ? null : code.trim();
    }

    private String fmt(LocalDateTime dt) {
        return (dt == null) ? "" : dt.atZone(APP_ZONE).format(TABLE_FMT);
    }

    private String fmtCsv(LocalDateTime dt) {
        return (dt == null) ? "" : dt.atZone(APP_ZONE).format(TABLE_FMT);
    }

    // ───────────────────────────────── Basic debug endpoint ──────────────────────

    // Mostly for debugging – not used by UI.
    @GetMapping
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    // ───────────────────────────────── Sign-In / Sign-Out ────────────────────────

    /**
     * Sign-In.
     * Request body: { "employeeCode": "E000001" }  (or legacy "employeeId")
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody Map<String, String> request) {
        String employeeCode = extractEmployeeCode(request);
        if (employeeCode == null || employeeCode.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Employee Code is required"));
        }

        Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(employeeCode);
        if (empOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Employee not found for code: " + employeeCode));
        }
        Employee emp = empOpt.get();

        // Check last record – if still open, do NOT allow another sign-in
        Optional<Attendance> lastOpt =
                attendanceRepository.findTopByEmployeeEmployeeCodeOrderBySignInTimeDesc(employeeCode);
        if (lastOpt.isPresent() && lastOpt.get().getSignOutTime() == null) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", "⚠️ Already signed in!"));
        }

        Attendance attendance = new Attendance();
        attendance.setEmployee(emp);
        attendance.setSignInTime(LocalDateTime.now(APP_ZONE));
        attendance.setSignOutTime(null);
        attendanceRepository.save(attendance);

        return ResponseEntity.ok(Map.of(
                "message", "✅ Sign-In recorded successfully",
                "employeeCode", employeeCode,
                "timestamp", fmt(attendance.getSignInTime())
        ));
    }

    /**
     * Sign-Out.
     * Request body: { "employeeCode": "E000001" }  (or legacy "employeeId")
     */
    @PostMapping("/signout")
    public ResponseEntity<?> signOut(@RequestBody Map<String, String> request) {
        String employeeCode = extractEmployeeCode(request);
        if (employeeCode == null || employeeCode.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Employee Code is required"));
        }

        Optional<Attendance> lastOpt =
                attendanceRepository.findTopByEmployeeEmployeeCodeOrderBySignInTimeDesc(employeeCode);

        if (lastOpt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "⚠️ No active session found for this employee!"));
        }

        Attendance latest = lastOpt.get();
        if (latest.getSignOutTime() != null) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", "⚠️ Already signed out!"));
        }

        latest.setSignOutTime(LocalDateTime.now(APP_ZONE));
        attendanceRepository.save(latest);

        return ResponseEntity.ok(Map.of(
                "message", "✅ Sign-Out recorded successfully",
                "employeeCode", employeeCode,
                "timestamp", fmt(latest.getSignOutTime())
        ));
    }

    // ───────────────────────────────── Active Users (Today) ──────────────────────

    /**
     * Used by Admin "Active Users (Today)" table.
     * Returns only rows where signOutTime is null and signInTime is today.
     */
    @GetMapping("/active-today")
    public List<Map<String, Object>> getActiveToday() {
        LocalDate today = LocalDate.now(APP_ZONE);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        List<Attendance> rows =
                attendanceRepository.findBySignOutTimeIsNullAndSignInTimeBetween(start, end);

        return rows.stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "employeeCode", a.getEmployee().getEmployeeCode(),
                        "name", a.getEmployee().getName(),
                        "signInTime", fmt(a.getSignInTime())
                ))
                .collect(Collectors.toList());
    }

    // ───────────────────────────────── Login History ─────────────────────────────

    /**
     * Used by Admin "Login History" table.
     * Optional filter: ?employeeCode=E000001
     */
    @GetMapping("/history")
    public List<Map<String, Object>> getHistory(
            @RequestParam(name = "employeeCode", required = false) String employeeCode) {

        List<Attendance> rows = attendanceRepository.findAllByOrderBySignInTimeDesc();

        if (employeeCode != null && !employeeCode.isBlank()) {
            String code = employeeCode.trim();
            rows = rows.stream()
                    .filter(a -> a.getEmployee() != null
                            && code.equalsIgnoreCase(a.getEmployee().getEmployeeCode()))
                    .collect(Collectors.toList());
        }

        return rows.stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId(),
                        "employeeCode", a.getEmployee().getEmployeeCode(),
                        "name", a.getEmployee().getName(),
                        "signInTime", fmt(a.getSignInTime()),
                        "signOutTime", fmt(a.getSignOutTime()),
                        "active", a.getSignOutTime() == null
                ))
                .collect(Collectors.toList());
    }

    // ───────────────────────────────── CSV Export: Monthly ───────────────────────

    /**
     * Export a month's attendance as CSV.
     *
     * GET /attendance/export-month?year=2025&month=1&employeeCode=E000001 (optional)
     * Month is 1–12.
     */
    @GetMapping("/export-month")
    public ResponseEntity<byte[]> exportMonthCsv(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(name = "employeeCode", required = false) String employeeCode) {

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = firstDay.plusMonths(1).atStartOfDay();

        List<Attendance> all = attendanceRepository.findAllByOrderBySignInTimeDesc();

        List<Attendance> filtered = all.stream()
                .filter(a -> a.getSignInTime() != null
                        && !a.getSignInTime().isBefore(start)
                        && a.getSignInTime().isBefore(end))
                .filter(a -> {
                    if (employeeCode == null || employeeCode.isBlank()) return true;
                    return a.getEmployee() != null &&
                           employeeCode.trim().equalsIgnoreCase(a.getEmployee().getEmployeeCode());
                })
                .sorted(Comparator.comparing(Attendance::getSignInTime))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("EmployeeCode,Name,SignIn,SignOut\n");
        for (Attendance a : filtered) {
            String code = a.getEmployee() != null ? a.getEmployee().getEmployeeCode() : "";
            String name = a.getEmployee() != null ? a.getEmployee().getName() : "";
            sb.append(code).append(',')
              .append(escapeCsv(name)).append(',')
              .append(escapeCsv(fmtCsv(a.getSignInTime()))).append(',')
              .append(escapeCsv(fmtCsv(a.getSignOutTime()))).append('\n');
        }

        String filename = String.format("attendance_%s_%02d_%s.csv",
                year, month,
                (employeeCode == null || employeeCode.isBlank())
                        ? "all"
                        : employeeCode.trim());

        return buildCsvResponse(filename, sb.toString());
    }

// ───────────────────────── CSV Export: Daily ─────────────────────────

/**
 * Export a single day's attendance as CSV.
 *
 * GET /attendance/export-day?day=2025-11-18 or 11/18/2025
 * Optional: &employeeCode=E000001
 *
 * Accepts day in either:
 *   - yyyy-MM-dd  (ISO, e.g. from <input type="date">)
 *   - MM/dd/yyyy  (US style, e.g. "11/18/2025")
 */
@GetMapping("/export-day")
public ResponseEntity<byte[]> exportDayCsv(
        @RequestParam(name = "day", required = false) String day,
        @RequestParam(name = "employeeCode", required = false) String employeeCode) {

    // If no day was provided at all, return a friendly 400 (NOT Spring's default one)
    if (day == null || day.isBlank()) {
        String msg = "Day is required. Please provide day as yyyy-MM-dd or MM/dd/yyyy.";
        byte[] errBytes = msg.getBytes(StandardCharsets.UTF_8);

        HttpHeaders errHeaders = new HttpHeaders();
        errHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        return ResponseEntity.badRequest().headers(errHeaders).body(errBytes);
    }

    // Try to parse day in two formats
    LocalDate date;
    try {
        // 1) ISO: 2025-11-18
        date = LocalDate.parse(day);
    } catch (Exception ex1) {
        try {
            // 2) US: 11/18/2025
            DateTimeFormatter usFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            date = LocalDate.parse(day, usFmt);
        } catch (Exception ex2) {
            String msg = "Invalid day format. Please use yyyy-MM-dd or MM/dd/yyyy. Got: " + day;
            byte[] errBytes = msg.getBytes(StandardCharsets.UTF_8);

            HttpHeaders errHeaders = new HttpHeaders();
            errHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
            return ResponseEntity.badRequest().headers(errHeaders).body(errBytes);
        }
    }

    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = start.plusDays(1);

    // Use existing history + filter for the chosen day
    List<Attendance> all = attendanceRepository.findAllByOrderBySignInTimeDesc();

    List<Attendance> filtered = all.stream()
            .filter(a -> a.getSignInTime() != null
                    && !a.getSignInTime().isBefore(start)
                    && a.getSignInTime().isBefore(end))
            .filter(a -> {
                if (employeeCode == null || employeeCode.isBlank()) return true;
                return a.getEmployee() != null &&
                        employeeCode.trim().equalsIgnoreCase(a.getEmployee().getEmployeeCode());
            })
            .sorted(Comparator.comparing(Attendance::getSignInTime))
            .collect(Collectors.toList());

    StringBuilder sb = new StringBuilder();
    sb.append("EmployeeCode,Name,SignIn,SignOut\n");
    for (Attendance a : filtered) {
        String code = a.getEmployee() != null ? a.getEmployee().getEmployeeCode() : "";
        String name = a.getEmployee() != null ? a.getEmployee().getName() : "";
        sb.append(code).append(',')
          .append(escapeCsv(name)).append(',')
          .append(escapeCsv(fmtCsv(a.getSignInTime()))).append(',')
          .append(escapeCsv(fmtCsv(a.getSignOutTime()))).append('\n');
    }

    String filename = String.format(
            "attendance_%s_%s.csv",
            date.format(CSV_DATE_FMT),
            (employeeCode == null || employeeCode.isBlank())
                    ? "all"
                    : employeeCode.trim()
    );

    return buildCsvResponse(filename, sb.toString());
}
    // ───────────────────────────────── CSV helpers ───────────────────────────────

    private String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    private ResponseEntity<byte[]> buildCsvResponse(String filename, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(bytes);
    }
}
