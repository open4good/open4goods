package org.open4goods.nudgerfrontapi.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Simple authentication service issuing random tokens.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final Map<String, String> refreshToAccess = new ConcurrentHashMap<>();

    public AuthService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public String token(String username, String password) {
        Authentication auth = new UsernamePasswordAuthenticationToken(username, password);
        authenticationManager.authenticate(auth);
        String access = UUID.randomUUID().toString();
        String refresh = UUID.randomUUID().toString();
        refreshToAccess.put(refresh, access);
        return access + ":" + refresh;
    }

    public String refresh(String refreshToken) {
        String access = refreshToAccess.get(refreshToken);
        if (access == null) {
            return null;
        }
        String newAccess = UUID.randomUUID().toString();
        refreshToAccess.put(refreshToken, newAccess);
        return newAccess;
    }
}
