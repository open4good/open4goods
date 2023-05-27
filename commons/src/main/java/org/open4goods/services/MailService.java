package org.open4goods.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * This service is in charge of alerting on events.
 *  Through "grouped" mail sending
 *  Through specific logging
 * @author Goulven.Furet
 *
 */
public class MailService {

	private static final Logger logger = LoggerFactory.getLogger(MailService.class);

	/**
	 * Thje java component used to send email
	 */
	private final JavaMailSender mailSender;

	



	public MailService( final JavaMailSender mailSender) {
		super();

		this.mailSender = mailSender;
	}


	/***
	 * @param to
	 * @param msg
	 * @param subject
	 * @param body
	 * @throws MessagingException
	 * @throws Exception
	 */
	public void sendEmail(final String to, final String msg, final String subject, String from) throws Exception{
		try {
			final MimeMessage message = mailSender.createMimeMessage();
			final MimeMessageHelper helper = new MimeMessageHelper(message);
			helper.setTo(to);
			helper.setText(msg);
			helper.setSubject(subject);
			helper.setFrom(from);
			helper.setReplyTo(from);
			mailSender.send(message);
		} catch (Exception e) {
			logger.error("Error while sending email ! ",e);
			throw e;

		}
	}



}