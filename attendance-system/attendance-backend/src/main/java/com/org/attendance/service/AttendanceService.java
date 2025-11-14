package com.org.attendance.service;

import com.org.attendance.model.Attendance;
import com.org.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    /**
     * Active users for today: signed in today, no sign-out yet.
     */
    public List<Attendance> getTodayActiveUsers() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return attendanceRepository.findBySignOutTimeIsNullAndSignInTimeBetween(start, end);
    }

    /**
     * Full login history ordered by latest sign-in first.
     */
    public List<Attendance> getLoginHistory() {
        return attendanceRepository.findAllByOrderBySignInTimeDesc();
    }

    /**
     * Latest attendance row for a given employee (by employeeCode).
     */
    public Optional<Attendance> findLatestByEmployeeCode(String employeeCode) {
        return attendanceRepository.findTopByEmployeeEmployeeCodeOrderBySignInTimeDesc(employeeCode);
    }
}

