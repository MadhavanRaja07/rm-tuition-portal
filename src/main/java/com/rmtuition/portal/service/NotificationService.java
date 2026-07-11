package com.rmtuition.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public void sendEmail(String to, String subject, String text) {
        if (mailSender == null || mailFrom.isEmpty() || mailFrom.contains("your_smtp")) {
            logger.warn("Mail Sender is not configured. Email to {} [Subject: '{}'] would have been sent, logging body:\n{}", to, subject, text);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Notification email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email notification to {}: {}", to, e.getMessage());
        }
    }

    public void sendPasswordResetLink(String to, String resetLink) {
        String subject = "Password Reset Link - RM Tuition Center";
        String body = "Dear Student/Teacher,\n\n" +
                "You requested a password reset. Please click the link below to change your password:\n" +
                resetLink + "\n\n" +
                "This link will expire soon.\n\n" +
                "Regards,\nRM Tuition Center Team";
        sendEmail(to, subject, body);
    }

    public void sendMaterialNotification(String studentEmail, String className, String subjectName, String title) {
        String subject = "New Material Uploaded - " + className;
        String body = "Hi Student,\n\n" +
                "A new material has been uploaded for your class (" + className + ").\n\n" +
                "Subject: " + subjectName + "\n" +
                "Title: " + title + "\n\n" +
                "Login to the portal to view and download the material.\n\n" +
                "Regards,\nRM Tuition Center Team";
        sendEmail(studentEmail, subject, body);
    }

    public void sendQuizNotification(String studentEmail, String className, String subjectName, String title) {
        String subject = "New Quiz Published - " + className;
        String body = "Hi Student,\n\n" +
                "A new quiz has been published for your class (" + className + ").\n\n" +
                "Subject: " + subjectName + "\n" +
                "Title: " + title + "\n\n" +
                "Log in to the student dashboard and complete the quiz before the deadline.\n\n" +
                "Regards,\nRM Tuition Center Team";
        sendEmail(studentEmail, subject, body);
    }

    public void sendPaperNotification(String studentEmail, String className, String subjectName, String title) {
        String subject = "New Practice Paper Uploaded - " + className;
        String body = "Hi Student,\n\n" +
                "A new question/practice paper has been uploaded for your class (" + className + ").\n\n" +
                "Subject: " + subjectName + "\n" +
                "Title: " + title + "\n\n" +
                "Download it from the question papers section in the student dashboard.\n\n" +
                "Regards,\nRM Tuition Center Team";
        sendEmail(studentEmail, subject, body);
    }
}
