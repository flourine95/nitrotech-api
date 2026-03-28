package com.nitrotech.api.infrastructure.mail;

import com.nitrotech.api.domain.auth.usecase.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ResendEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String from;

    public ResendEmailSender(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from}") String from
    ) {
        this.from = from;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public void sendPasswordReset(String to, String resetLink) {
        send(to, "Reset your password",
                "<p>Click the link below to reset your password (expires in 15 minutes):</p>" +
                "<p><a href=\"" + resetLink + "\">Reset Password</a></p>" +
                "<p>If you didn't request this, ignore this email.</p>");
    }

    private void send(String to, String subject, String html) {
        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmailRequest(from, List.of(to), subject, html))
                .retrieve()
                .toBodilessEntity();
    }

    private record EmailRequest(String from, List<String> to, String subject, String html) {}
}
