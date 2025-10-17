package com.org.attendance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String company;
    private String project;
    private String skill;
    private String department;

    // Constructors
    public Employee() {}
    
    public Employee(String name, String email, String company, String project, String skill, String department) {
        this.name = name;
        this.email = email;
        this.company = company;
        this.project = project;
        this.skill = skill;
        this.department = department;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}

