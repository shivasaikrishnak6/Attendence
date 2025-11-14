package com.org.attendance.controller;

import com.org.attendance.model.Employee;
import com.org.attendance.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    // GET /employees -> list all
    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // GET /employees/{id} -> by numeric id
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Employee not found")));
    }

    // GET /employees/code/{employeeCode} -> by employeeCode
    @GetMapping("/code/{employeeCode}")
    public ResponseEntity<?> getByCode(@PathVariable String employeeCode) {
        Optional<Employee> emp = employeeRepository.findByEmployeeCode(employeeCode);
        return emp.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Employee not found for code: " + employeeCode)));
    }

    // POST /employees -> create (expects JSON with at least name and employeeCode)
    @PostMapping
    public ResponseEntity<?> addEmployee(@RequestBody Employee employee) {
        try {
            // Basic validation
            if (employee.getName() == null || employee.getName().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "name is required"));
            }
            if (employee.getEmployeeCode() == null || employee.getEmployeeCode().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "employeeCode is required"));
            }

            // Uniqueness check for code
            if (employeeRepository.existsByEmployeeCode(employee.getEmployeeCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "employeeCode already exists"));
            }

            Employee saved = employeeRepository.save(employee);
            return ResponseEntity
                    .created(URI.create("/employees/" + saved.getId()))
                    .body(saved);

        } catch (DataIntegrityViolationException e) {
            // Covers unique email/code constraint collisions as a fallback
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Duplicate field (email or employeeCode)"));
        }
    }

    // PUT /employees/{id} -> update minimal fields (optional)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody Employee incoming) {
        Optional<Employee> opt = employeeRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Employee not found"));
        }

        Employee e = opt.get();
        if (incoming.getName() != null) e.setName(incoming.getName());
        if (incoming.getEmail() != null) e.setEmail(incoming.getEmail());
        if (incoming.getEmployeeCode() != null) {
            // If changing code, ensure uniqueness
            if (!incoming.getEmployeeCode().equals(e.getEmployeeCode())
                    && employeeRepository.existsByEmployeeCode(incoming.getEmployeeCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "employeeCode already exists"));
            }
            e.setEmployeeCode(incoming.getEmployeeCode());
        }

        try {
            Employee saved = employeeRepository.save(e);
            return ResponseEntity.ok(saved);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Duplicate field (email or employeeCode)"));
        }
    }

    // DELETE /employees/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Employee not found"));
        }
        employeeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Employee deleted"));
    }
}

