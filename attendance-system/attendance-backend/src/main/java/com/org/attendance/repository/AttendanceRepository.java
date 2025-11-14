package com.org.attendance.repository;

import com.org.attendance.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * Latest attendance row for a given employee, looked up by employeeCode.
     */
    Optional<Attendance> findTopByEmployeeEmployeeCodeOrderBySignInTimeDesc(String employeeCode);

    /**
     * Employees who are currently signed in today (no sign_out_time yet).
     */
    List<Attendance> findBySignOutTimeIsNullAndSignInTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Full login history ordered by latest sign-in first.
     */
    List<Attendance> findAllByOrderBySignInTimeDesc();

    /**
     * Login history for a single employee ordered by latest sign-in first.
     */
    List<Attendance> findByEmployeeEmployeeCodeOrderBySignInTimeDesc(String employeeCode);

    /**
     * All attendance rows in a given date/time range (for CSV export, etc.).
     */
    List<Attendance> findBySignInTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Attendance rows for a single employee in a date/time range.
     */
    List<Attendance> findByEmployeeEmployeeCodeAndSignInTimeBetween(
            String employeeCode,
            LocalDateTime start,
            LocalDateTime end
    );
}

