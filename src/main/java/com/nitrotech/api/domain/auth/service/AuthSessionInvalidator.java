package com.nitrotech.api.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class AuthSessionInvalidator {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public void invalidateByEmail(String email) {
        sessionRepository.findByPrincipalName(email)
                .values()
                .forEach(session -> sessionRepository.deleteById(session.getId()));
    }

    public void invalidateByEmails(Collection<String> emails) {
        emails.forEach(this::invalidateByEmail);
    }
}
