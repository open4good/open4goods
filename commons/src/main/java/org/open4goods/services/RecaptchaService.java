package org.open4goods.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.helper.RecaptchaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;

/**
 * This service allow Google Recaptcha validation
 *
 * @author Goulven.Furet
 *
 */
public class RecaptchaService {


	private static final String GOOGLE_RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

	@Value("${google.recaptcha.secret}")
	String recaptchaSecret;

	@Autowired
	RestTemplateBuilder restTemplateBuilder;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void verifyRecaptcha(final String ip, final String recaptchaResponse) throws SecurityException {
		final Map<String, String> body = new HashMap<>();
		body.put("secret", recaptchaSecret);
		body.put("response", recaptchaResponse);
		body.put("remoteip", ip);

		final ResponseEntity<Map> recaptchaResponseEntity = restTemplateBuilder.build().postForEntity(
				GOOGLE_RECAPTCHA_VERIFY_URL + "?secret={secret}&response={response}&remoteip={remoteip}", body,
				Map.class, body);

		final Map<String, Object> responseBody = recaptchaResponseEntity.getBody();

		final boolean recaptchaSucess = (Boolean) responseBody.get("success");
		if (!recaptchaSucess) {
			final List<String> errorCodes = (List) responseBody.get("error-codes");

			final String errorMessage = errorCodes.stream().map(RecaptchaUtil.RECAPTCHA_ERROR_CODE::get)
					.collect(Collectors.joining(", "));

			throw new SecurityException(errorMessage);
		}
	}


}