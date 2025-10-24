package com.org.attendance.controller;

import com.org.attendance.model.Attendance;
import com.org.attendance.model.Employee;
import com.org.attendance.repository.AttendanceRepository;
import com.org.attendance.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // 🧾 Get all attendance records (for admin dashboard)
    @GetMapping
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    // 🟢 Sign In (JSON body: { "employeeId": "123" })
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody Map<String, String> request) {
        try {
            Long employeeId = Long.parseLong(request.get("employeeId"));

            Optional<Employee> emp = employeeRepository.findById(employeeId);
            if (emp.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "❌ Employee not found!"));
            }

            Attendance attendance = new Attendance();
            attendance.setEmployee(emp.get());
            attendance.setSignInTime(LocalDateTime.now());
            attendanceRepository.save(attendance);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Sign-In recorded successfully",
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid request format or missing employeeId"));
        }
    }

    // 🔴 Sign Out (JSON body: { "employeeId": "123" })
    @PostMapping("/signout")
    public ResponseEntity<?> signOut(@RequestBody Map<String, String> request) {
        try {
            Long employeeId = Long.parseLong(request.get("employeeId"));

            Optional<Attendance> attendance = attendanceRepository.findTopByEmployeeIdOrderBySignInTimeDesc(employeeId);
            if (attendance.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "⚠️ No active session found for this employee!"));
            }

            Attendance latest = attendance.get();
            if (latest.getSignOutTime() != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "⚠️ Already signed out!"));
            }

            latest.setSignOutTime(LocalDateTime.now());
            attendanceRepository.save(latest);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Sign-Out recorded successfully",
                    "timestamp", LocalDateTime.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid request format or missing employeeId"));
        }
    }

    // 🧩 Manual add (optional)
    @PostMapping
    public Attendance addAttendance(@RequestBody Attendance attendance) {
        return attendanceRepository.save(attendance);
    }
}

