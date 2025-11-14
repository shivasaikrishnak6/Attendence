-- =====================================================================
-- Attendance Tracking DB bootstrap
-- Place this file at: ./attendance-backend/init.sql
-- NOTE: This runs only on a fresh DB volume (first startup).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS attendance_db;
USE attendance_db;

-- =====================================================================
-- Tables used by the Spring Boot JPA entities
-- =====================================================================

-- Employee master (used by Employee.java)
CREATE TABLE IF NOT EXISTS employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    employee_code VARCHAR(64) NOT NULL,
    CONSTRAINT ux_employee_email UNIQUE (email),
    CONSTRAINT ux_employee_code  UNIQUE (employee_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Attendance records (used by Attendance.java)
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    sign_in_time  DATETIME NULL,
    sign_out_time DATETIME NULL,
    -- 0 = normal working day, 1 = holiday (or however you want to interpret it)
    holiday TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_att_employee
        FOREIGN KEY (employee_id) REFERENCES employee(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed some demo employees for the JPA tables
INSERT INTO employee (name, email, employee_code) VALUES
('Alice Johnson',  'alice.hr@company.com',      'E000001'),
('Bob Smith',      'bob.dev@company.com',       'E000002'),
('Charlie Davis',  'charlie.finance@company.com','E000003');

-- Optional: seed a couple of attendance records (today)
INSERT INTO attendance (employee_id, sign_in_time, sign_out_time)
SELECT e.id, NOW(), NULL
FROM employee e
WHERE e.employee_code IN ('E000001','E000002');

-- =====================================================================
-- Legacy demo tables (kept for compatibility with your old sample data)
-- These are NOT used by JPA, but safe to keep if you still need them.
-- =====================================================================

CREATE TABLE IF NOT EXISTS employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50),
    role VARCHAR(50),
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS attendance_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    status ENUM('Present', 'Absent', 'Late', 'On Leave') DEFAULT 'Present',
    check_in_time TIME,
    check_out_time TIME,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed legacy-demo data (unrelated to JPA tables above)
INSERT INTO employees (employee_id, name, department, role, email) VALUES
('EMP001', 'Alice Johnson',  'HR',         'Manager',  'alice.hr@company.com'),
('EMP002', 'Bob Smith',      'Engineering','Developer','bob.dev@company.com'),
('EMP003', 'Charlie Davis',  'Finance',    'Analyst',  'charlie.finance@company.com');

INSERT INTO attendance_records (employee_id, date, status, check_in_time, check_out_time) VALUES
('EMP001', CURDATE(), 'Present', '09:00:00', '17:00:00'),
('EMP002', CURDATE(), 'Late',    '09:45:00', '17:30:00'),
('EMP003', CURDATE(), 'Present', '09:10:00', '17:15:00');

