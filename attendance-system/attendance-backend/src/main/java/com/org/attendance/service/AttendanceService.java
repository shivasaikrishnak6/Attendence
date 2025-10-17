package com.org.attendance.service;

import com.org.attendance.model.Attendance;
import com.org.attendance.model.Employee;
import com.org.attendance.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public Attendance signIn(Employee employee) {
        LocalDate today = LocalDate.now();
        boolean isWeekend = today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY;

        Attendance attendance = new Attendance();
        attendance.setDate(today);
        attendance.setSignInTime(LocalTime.now());
        attendance.setHoliday(isWeekend);
        attendance.setEmployee(employee);
        return attendanceRepository.save(attendance);
    }

    public Attendance signOut(Employee employee) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findAll()
                .stream()
                .filter(a -> a.getEmployee().getId().equals(employee.getId()) && a.getDate().equals(today))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Sign-in not found!"));

        attendance.setSignOutTime(LocalTime.now());
        return attendanceRepository.save(attendance);
    }
}

