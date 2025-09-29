package org.open4goods.nudgerfrontapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Thin wrapper around {@link JavaMailSender} dedicated to contact form emails.
 */
@Service
public class ContactMailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactMailService.class);

    private final JavaMailSender mailSender;

    public ContactMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send a plain text email using the configured mail sender.
     *
     * @param to recipient email address
     * @param body message body
     * @param subject email subject line
     * @param from sender email address used for reply-to
     * @throws Exception when the underlying mail sender cannot dispatch the message
     */
    public void send(String to, String body, String subject, String from) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setTo(to);
        helper.setText(body);
        helper.setSubject(subject);
        helper.setFrom(from);
        helper.setReplyTo(from);
        mailSender.send(message);
        LOGGER.debug("Contact email sent to {} with subject {}", to, subject);
    }
}
