package com.org.attendance.repository;

import com.org.attendance.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    @Query("SELECT s FROM UserSession s WHERE s.active = true")
    List<UserSession> findActive();

    @Query("SELECT s FROM UserSession s ORDER BY s.loginTime DESC")
    List<UserSession> findAllOrderByLoginTimeDesc();

    @Query("SELECT s FROM UserSession s WHERE s.username = ?1 AND s.active = true ORDER BY s.loginTime DESC")
    List<UserSession> findActiveByUsername(String username);
}

