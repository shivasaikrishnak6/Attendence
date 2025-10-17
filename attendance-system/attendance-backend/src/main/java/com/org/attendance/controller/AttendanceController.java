package com.org.attendance.controller;

import com.org.attendance.model.Attendance;
import com.org.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attendances")  // Fixed: must not be /employees
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    // Get all attendance records
    @GetMapping
    public List<Attendance> getAllAttendances() {
        return attendanceRepository.findAll();
    }

    // Add a new attendance record
    @PostMapping
    public Attendance addAttendance(@RequestBody Attendance attendance) {
        return attendanceRepository.save(attendance);
    }
}

