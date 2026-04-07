package com.nitrotech.api.domain.auth.usecase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class LogoutUseCase {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public LogoutUseCase(FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public void execute(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    public void executeAll(String email, HttpServletRequest request) {
        sessionRepository.findByPrincipalName(email)
                .values()
                .forEach(s -> sessionRepository.deleteById(s.getId()));
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
}
