package com.org.attendance.model;

import java.time.LocalDateTime;

public class EmployeeSessionView {

    private Long id;
    private String username;      // will show "CODE - Name"
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private boolean active;

    public EmployeeSessionView(Long id,
                               String username,
                               LocalDateTime loginTime,
                               LocalDateTime logoutTime,
                               boolean active) {
        this.id = id;
        this.username = username;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.active = active;
    }

    // Helper factory from Attendance entity
    public static EmployeeSessionView from(Attendance a) {
        Employee e = a.getEmployee();
        String code = (e != null && e.getEmployeeCode() != null) ? e.getEmployeeCode() : "";
        String name = (e != null && e.getName() != null) ? e.getName() : "";
        String label = (code.isEmpty() && name.isEmpty())
                ? "Unknown"
                : (code.isEmpty() ? name : (name.isEmpty() ? code : code + " - " + name));

        return new EmployeeSessionView(
                a.getId(),
                label,
                a.getSignInTime(),
                a.getSignOutTime(),
                a.getSignOutTime() == null   // active if no sign-out
        );
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public boolean isActive() {
        return active;
    }
}

