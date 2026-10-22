package org.open4goods.nudgerfrontapi.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.open4goods.nudgerfrontapi.dto.auth.GoogleSsoLoginRequest;
import org.open4goods.nudgerfrontapi.service.auth.GoogleIdentityService;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.open4goods.nudgerfrontapi.service.exception.GoogleIdentityVerificationException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;

/**
 * Unit tests for the Google SSO refusal logging of {@link AuthController}.
 */
@ExtendWith(OutputCaptureExtension.class)
class AuthControllerTest {

    private static final String ID_TOKEN = "header.payload-that-must-not-be-logged.signature";
    private static final String NONCE = "nonce-that-must-not-be-logged";

    private final JwtService jwtService = mock(JwtService.class);
    private final GoogleIdentityService googleIdentityService = mock(GoogleIdentityService.class);
    private final AuthController controller = new AuthController(jwtService, googleIdentityService);

    @Test
    void logsRefusalReasonWithoutTokenOrNonce(CapturedOutput output) {
        given(googleIdentityService.verify(ID_TOKEN, NONCE)).willThrow(new GoogleIdentityVerificationException(
                "Email j***@example.com is not present in the environment role allowlist (1 entries)"));

        var response = controller.loginWithGoogle(new GoogleSsoLoginRequest(ID_TOKEN, NONCE));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(output).contains("WARN")
                .contains("Google SSO login refused (401): Email j***@example.com is not present")
                .doesNotContain(ID_TOKEN)
                .doesNotContain(NONCE);
    }

    @Test
    void logsCauseOfTokenVerificationFailure(CapturedOutput output) {
        given(googleIdentityService.verify(ID_TOKEN, NONCE)).willThrow(new GoogleIdentityVerificationException(
                "Google ID token verification failed", new BadJwtException("Jwt expired")));

        var response = controller.loginWithGoogle(new GoogleSsoLoginRequest(ID_TOKEN, NONCE));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(output).contains("Google ID token verification failed (BadJwtException: Jwt expired)")
                .doesNotContain(ID_TOKEN);
    }
}
