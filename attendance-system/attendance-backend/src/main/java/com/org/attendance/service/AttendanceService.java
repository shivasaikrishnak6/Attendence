package com.org.attendance.service;

import com.org.attendance.model.Attendance;
import com.org.attendance.repository.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Latest attendance row for a given employee code.
     */
    public Optional<Attendance> findLatestByEmployeeCode(String employeeCode) {
        return attendanceRepository
                .findTopByEmployeeEmployeeCodeOrderBySignInTimeDesc(employeeCode);
    }

    /**
     * Employees who are currently signed in today (no sign-out yet).
     */
    public List<Attendance> getTodayActiveUsers() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusNanos(1);
        return attendanceRepository.findBySignOutTimeIsNullAndSignInTimeBetween(start, end);
    }

    /**
     * Full login history ordered by latest sign-in first.
     */
    public List<Attendance> getLoginHistory() {
        return attendanceRepository.findAllByOrderBySignInTimeDesc();
    }

    /**
     * All attendance rows for a specific calendar date, optionally filtered by employee code.
     */
    public List<Attendance> getAttendanceForDate(LocalDate date, Optional<String> employeeCodeOpt) {
        List<Attendance> all = attendanceRepository.findAllByOrderBySignInTimeDesc();

        return all.stream()
                .filter(a -> a.getSignInTime() != null
                        && a.getSignInTime().toLocalDate().equals(date))
                .filter(a -> employeeCodeOpt
                        .map(code -> code.equalsIgnoreCase(a.getEmployee().getEmployeeCode()))
                        .orElse(true))
                .toList();
    }

    /**
     * All attendance rows for a specific month, optionally filtered by employee code.
     */
    public List<Attendance> getAttendanceForMonth(int year, int month, Optional<String> employeeCodeOpt) {
        List<Attendance> all = attendanceRepository.findAllByOrderBySignInTimeDesc();

        return all.stream()
                .filter(a -> {
                    if (a.getSignInTime() == null) return false;
                    LocalDate d = a.getSignInTime().toLocalDate();
                    return d.getYear() == year && d.getMonthValue() == month;
                })
                .filter(a -> employeeCodeOpt
                        .map(code -> code.equalsIgnoreCase(a.getEmployee().getEmployeeCode()))
                        .orElse(true))
                .toList();
    }
}
