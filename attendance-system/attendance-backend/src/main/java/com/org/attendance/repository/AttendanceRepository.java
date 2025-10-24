package com.org.attendance.repository;

import com.org.attendance.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 🔍 Finds the latest attendance record for a specific employee (used for Sign Out)
    Optional<Attendance> findTopByEmployeeIdOrderBySignInTimeDesc(Long employeeId);
}

