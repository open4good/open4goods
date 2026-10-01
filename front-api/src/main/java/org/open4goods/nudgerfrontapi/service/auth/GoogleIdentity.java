package org.open4goods.nudgerfrontapi.service.auth;

import java.util.List;

/**
 * Normalized identity resolved from a verified Google ID token, carrying the
 * roles granted by the environment allowlist.
 */
public record GoogleIdentity(String email, List<String> roles) {
}
