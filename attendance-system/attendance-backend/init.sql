CREATE DATABASE IF NOT EXISTS attendance_db;
USE attendance_db;

CREATE TABLE IF NOT EXISTS employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50),
    role VARCHAR(50),
    email VARCHAR(100) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attendance_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    date DATE NOT NULL,
    status ENUM('Present', 'Absent', 'Late', 'On Leave') DEFAULT 'Present',
    check_in_time TIME,
    check_out_time TIME,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
);

INSERT INTO employees (employee_id, name, department, role, email)
VALUES 
('EMP001', 'Alice Johnson', 'HR', 'Manager', 'alice.hr@company.com'),
('EMP002', 'Bob Smith', 'Engineering', 'Developer', 'bob.dev@company.com'),
('EMP003', 'Charlie Davis', 'Finance', 'Analyst', 'charlie.finance@company.com');

INSERT INTO attendance_records (employee_id, date, status, check_in_time, check_out_time)
VALUES 
('EMP001', CURDATE(), 'Present', '09:00:00', '17:00:00'),
('EMP002', CURDATE(), 'Late', '09:45:00', '17:30:00'),
('EMP003', CURDATE(), 'Present', '09:10:00', '17:15:00');

