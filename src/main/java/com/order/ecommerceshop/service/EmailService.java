package com.order.ecommerceshop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService
{
    private final JavaMailSender javaMailSender;


    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value(("${app.frontend.url}"))
    private String frontendUrl;


    // password reset email send
    public void sendPasswordResetEmail(String toEmail, String token)
    {
        String resetLink = frontendUrl + "/?token=" + token;

        try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(fromEmail);
                mailMessage.setTo(toEmail);
                mailMessage.setSubject("MyStore - Password Reset Request");
            mailMessage.setText(
                    "Hello,\n\n" +
                            "You requested to reset your password for MyStore.\n\n" +
                            "Click the link below to reset your password:\n\n" +
                            resetLink + "\n\n" +
                            "This link will expire in 15 minutes.\n\n" +
                            "If you didn't request this, please ignore this email.\n\n" +
                            "Thanks,\nMyStore Team"
            );
                javaMailSender.send(mailMessage);
                log.info("password reset email send to {}", toEmail);
            }
            catch(Exception ex)
            {
                log.error("Filed to send email {}: {}", toEmail, ex.getMessage());

                // for development console
                log.info("Reset link: {}", resetLink);
            }

    }
}
