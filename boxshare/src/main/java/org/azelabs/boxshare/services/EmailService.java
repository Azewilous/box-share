package org.azelabs.boxshare.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendVerificationEmail(String to, UUID token) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Verify your Boxshare email");
        msg.setText("Click the link below to verify your account:\n\n"
                + frontendUrl + "/verify-email?token=" + token
                + "\n\nThis link expires in 24 hours.");
        mailSender.send(msg);
    }
}
