package com.org.attendance.service;

import com.org.attendance.model.Attendance;
import com.org.attendance.repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private static final ZoneId TZ_NY = ZoneId.of("America/New_York");

    @Autowired
    private AttendanceRepository attendanceRepository;

    public Optional<Attendance> findLatestByEmployeeCode(String employeeCode) {
        return attendanceRepository.findTopByEmployeeEmployeeCodeOrderBySignInTimeDesc(employeeCode);
    }

    /**
     * Active users for "today" in America/New_York timezone.
     */
    public List<Attendance> getTodayActiveUsers() {
        LocalDate today = LocalDate.now(TZ_NY);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);
        return attendanceRepository.findBySignOutTimeIsNullAndSignInTimeBetween(start, end);
    }

    /**
     * Full login history (all employees).
     */
    public List<Attendance> getLoginHistory() {
        return attendanceRepository.findAllByOrderBySignInTimeDesc();
    }

    /**
     * Login history for a single employee.
     */
    public List<Attendance> getLoginHistoryForEmployee(String employeeCode) {
        return attendanceRepository.findByEmployeeEmployeeCodeOrderBySignInTimeDesc(employeeCode);
    }
}

