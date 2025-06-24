package org.open4goods.nudgerfrontapi.dto;

/**
 * Authentication request containing credentials.
 */
public record AuthRequest(String username, String password) {
}
