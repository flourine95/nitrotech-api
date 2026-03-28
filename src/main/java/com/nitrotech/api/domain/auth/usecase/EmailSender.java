package com.nitrotech.api.domain.auth.usecase;

public interface EmailSender {
    void sendPasswordReset(String to, String resetLink);
}
