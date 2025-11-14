package com.org.attendance.controller;

import com.org.attendance.model.Attendance;
import com.org.attendance.model.Employee;
import com.org.attendance.repository.AttendanceRepository;
import com.org.attendance.repository.EmployeeRepository;
import com.org.attendance.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    // 🧾  Get all raw attendance rows (mostly for debugging)
    @GetMapping
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

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

    // 🟢  Sign In  (JSON: { "employeeId": "BCG5656" } or { "employeeCode": "BCG5656" })
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody Map<String, String> request) {
        String employeeCode = extractEmployeeCode(request);
        if (employeeCode == null || employeeCode.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "employeeId (employeeCode) is required"));
        }

        Optional<Employee> empOpt = employeeRepository.findByEmployeeCode(employeeCode);
        if (empOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "❌  Employee not found for code: " + employeeCode));
        }

        Employee emp = empOpt.get();

        Attendance attendance = new Attendance();
        attendance.setEmployee(emp);
        attendance.setSignInTime(LocalDateTime.now());
        attendance.setSignOutTime(null);

        attendanceRepository.save(attendance);

        return ResponseEntity.ok(Map.of(
                "message", "✅  Sign-In recorded successfully",
                "employeeCode", employeeCode,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // 🔴  Sign Out (JSON: { "employeeId": "BCG5656" } or { "employeeCode": "BCG5656" })
    @PostMapping("/signout")
    public ResponseEntity<?> signOut(@RequestBody Map<String, String> request) {
        String employeeCode = extractEmployeeCode(request);
        if (employeeCode == null || employeeCode.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "employeeId (employeeCode) is required"));
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
                "message", "✅  Sign-Out recorded successfully",
                "employeeCode", employeeCode,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // 📊 Active users today (for Admin "Active Users" panel if you want)
    @GetMapping("/active-today")
    public List<Map<String, Object>> getActiveToday() {
        return attendanceService.getTodayActiveUsers()
                .stream()
                .map(a -> Map.<String, Object>of(
                        "employeeCode", a.getEmployee().getEmployeeCode(),
                        "name", a.getEmployee().getName(),
                        "signInTime", a.getSignInTime()
                ))
                .collect(Collectors.toList());
    }

    // 📜 Full login history (for Admin "Login History" panel if you want)
    @GetMapping("/history")
    public List<Map<String, Object>> getHistory() {
        return attendanceService.getLoginHistory()
                .stream()
                .map(a -> Map.<String, Object>of(
                        "employeeCode", a.getEmployee().getEmployeeCode(),
                        "name", a.getEmployee().getName(),
                        "signInTime", a.getSignInTime(),
                        "signOutTime", a.getSignOutTime()
                ))
                .collect(Collectors.toList());
    }
}

