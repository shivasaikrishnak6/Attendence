package com.org.attendance.service;

import com.org.attendance.model.UserSession;
import com.org.attendance.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionService {
    private final UserSessionRepository repo;
    public SessionService(UserSessionRepository repo) { this.repo = repo; }

    @Transactional
    public UserSession signIn(String username) {
        // close any dangling active sessions for this user
        repo.findActiveByUsername(username).forEach(s -> {
            s.setActive(false);
            s.setLogoutTime(LocalDateTime.now());
        });
        return repo.save(new UserSession(username));
    }

    @Transactional
    public void signOut(String username) {
        repo.findActiveByUsername(username).forEach(s -> {
            s.setActive(false);
            s.setLogoutTime(LocalDateTime.now());
        });
    }

    public List<UserSession> getActive() { return repo.findActive(); }
    public List<UserSession> getHistory() { return repo.findAllOrderByLoginTimeDesc(); }
}

