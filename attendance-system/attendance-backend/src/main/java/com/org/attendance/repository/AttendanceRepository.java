package com.org.attendance.repository;

import com.org.attendance.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * Latest attendance row for a given employee, looked up by employeeCode.
     * (employee is a relation on Attendance)
     */
    Optional<Attendance> findTopByEmployeeEmployeeCodeOrderBySignInTimeDesc(String employeeCode);

    /**
     * Employees who are currently signed in in a given time window.
     */
    List<Attendance> findBySignOutTimeIsNullAndSignInTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Full login history ordered by latest sign-in first.
     */
    List<Attendance> findAllByOrderBySignInTimeDesc();

    /**
     * All rows in a given date/time range (used for monthly & daily CSV export).
     */
    List<Attendance> findBySignInTimeBetween(LocalDateTime start, LocalDateTime end);
}
