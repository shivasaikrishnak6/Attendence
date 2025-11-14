package com.org.attendance.repository;

import com.org.attendance.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmployeeCode(String employeeCode);
}

