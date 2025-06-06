package org.open4goods.services.captcha.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.services.captcha.HcaptchaUtil;
import org.open4goods.services.captcha.config.HcaptchaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service that performs hCaptcha Recaptcha validation and assigns a spring role to users
 * upon successful verification.
 * <p>
 * This class uses the {@link HcaptchaProperties} configuration to retrieve the secret key
 * and the role that must be assigned to a valid user.
 * </p>
 *
 * @author Goulven
 */
@Service
public class HcaptchaService{

    private static final String H_RECAPTCHA_VERIFY_URL = "https://api.hcaptcha.com/siteverify";
    private static final Logger logger = LoggerFactory.getLogger(HcaptchaService.class);

    private final HcaptchaProperties captchaProperties;
    private final RestTemplate restTemplate;

    /**
     * Constructor that initializes the service with externalized captcha properties and a RestTemplate.
     *
     * @param captchaProperties the configuration properties for captcha verification
     * @param restTemplateBuilder the builder to create a RestTemplate instance
     */
    public HcaptchaService(HcaptchaProperties captchaProperties, RestTemplateBuilder restTemplateBuilder) {
        this.captchaProperties = captchaProperties;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Verifies the captcha response and, if valid, assigns the configured spring role to the current user.
     *
     * @param ip the remote IP address of the user
     * @param recaptchaResponse the captcha response token
     * @throws SecurityException if captcha verification fails
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void verifyRecaptcha(final String ip, final String recaptchaResponse) throws SecurityException {
        final Map<String, String> body = new HashMap<>();
        body.put("secret", captchaProperties.secretKey());
        body.put("response", recaptchaResponse);
        body.put("remoteip", ip);

        final ResponseEntity<Map> recaptchaResponseEntity = restTemplate.postForEntity(
                H_RECAPTCHA_VERIFY_URL + "?secret={secret}&response={response}&remoteip={remoteip}",
                body, Map.class, body);

        final Map<String, Object> responseBody = recaptchaResponseEntity.getBody();
        final boolean recaptchaSuccess = (Boolean) responseBody.get("success");

        if (!recaptchaSuccess) {
            final List<String> errorCodes = (List) responseBody.get("error-codes");
            final String errorMessage = errorCodes.stream()
                    .map(HcaptchaUtil.RECAPTCHA_ERROR_CODE::get)
                    .collect(Collectors.joining(", "));
            logger.warn("Captcha verification failed: {}", errorMessage);
            throw new SecurityException(errorMessage);
        }

        // On successful captcha verification, assign the configured spring role to the user
        assignRoleToUser(captchaProperties.validRole());
        logger.info("Captcha verified successfully, assigned role: {}", captchaProperties.validRole());
    }

    /**
     * Updates the current Spring Security context by adding the given role to the user's granted authorities.
     *
     * @param role the role to assign to the authenticated user
     */
    private void assignRoleToUser(String role) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.isAuthenticated()) {
            // Copy current authorities and add new role if not already present
            List<GrantedAuthority> updatedAuthorities = new ArrayList<>(currentAuth.getAuthorities());
            SimpleGrantedAuthority newAuthority = new SimpleGrantedAuthority(role);
            if (updatedAuthorities.stream().noneMatch(auth -> auth.getAuthority().equals(newAuthority.getAuthority()))) {
                updatedAuthorities.add(newAuthority);
            }
            // Create a new authentication token with the updated authorities
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    currentAuth.getPrincipal(), currentAuth.getCredentials(), updatedAuthorities);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            logger.debug("Updated authentication authorities: {}", updatedAuthorities);
        } else {
            logger.warn("No authenticated user found. Role {} not assigned.", role);
        }
    }


}
