package com.org.attendance.service;

import com.org.attendance.model.Attendance;
import com.org.attendance.model.Employee;
import com.org.attendance.repository.AttendanceRepository;
import com.org.attendance.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // ✅ Sign In
    public Attendance signIn(Long employeeId) {
        Optional<Employee> empOpt = employeeRepository.findById(employeeId);
        if (empOpt.isEmpty()) throw new RuntimeException("Employee not found");

        Attendance attendance = new Attendance();
        attendance.setEmployee(empOpt.get());
        attendance.setSignInTime(LocalDateTime.now());
        attendance.setSignOutTime(null); // Not yet signed out

        return attendanceRepository.save(attendance);
    }

    // ✅ Sign Out
    public Attendance signOut(Long employeeId) {
        Optional<Attendance> latestOpt = attendanceRepository.findTopByEmployeeIdOrderBySignInTimeDesc(employeeId);
        if (latestOpt.isEmpty()) throw new RuntimeException("No active session found");

        Attendance latest = latestOpt.get();
        latest.setSignOutTime(LocalDateTime.now());

        return attendanceRepository.save(latest);
    }

    // ✅ Get all attendance records
    public List<Attendance> getAll() {
        return attendanceRepository.findAll();
    }
}

