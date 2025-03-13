package org.open4goods.services.captcha.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.services.captcha.CaptchaVerificationException;
import org.open4goods.services.captcha.config.HcaptchaProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;

public class HcaptchaServiceTest {

    private RestTemplate restTemplate;
    private RestTemplateBuilder restTemplateBuilder;
    private HcaptchaProperties captchaProperties;
    private HcaptchaService hcaptchaService;

    @BeforeEach
    public void setUp() {
        captchaProperties = new HcaptchaProperties();
        captchaProperties.setSecretKey("dummySecret");
        captchaProperties.setValidRole("ROLE_CAPTCHA_VERIFIED");

        restTemplate = Mockito.mock(RestTemplate.class);
        restTemplateBuilder = Mockito.mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        // Set up a dummy authentication in the security context
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "pass", new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(auth);

        hcaptchaService = new HcaptchaService(captchaProperties, restTemplateBuilder);
    }

    @Test
    public void testVerifyRecaptchaSuccess() {
        // Prepare a mock response for a successful captcha verification
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(responseMap);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class), any(Map.class)))
                .thenReturn(responseEntity);

        assertDoesNotThrow(() -> hcaptchaService.verifyRecaptcha("127.0.0.1", "dummyResponse"));

        // Check that the role has been assigned
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CAPTCHA_VERIFIED"));
        assertTrue(hasRole);
    }

    @Test
    public void testVerifyRecaptchaFailure() {
        // Prepare a mock response for a failed captcha verification
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", false);
        responseMap.put("error-codes", Arrays.asList("missing-input-response"));
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(responseMap);
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class), any(Map.class)))
                .thenReturn(responseEntity);

        SecurityException exception = assertThrows(SecurityException.class,
                () -> hcaptchaService.verifyRecaptcha("127.0.0.1", "dummyResponse"));
        assertTrue(exception.getMessage().contains("The response parameter is missing"));
    }
}
