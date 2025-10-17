package com.org.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.org.attendance.model.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {}

