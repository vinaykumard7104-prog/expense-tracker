package com.expensetracker.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private final JwtService jwt = new JwtService("unit-test-secret-0123456789-abcdefghij", 60_000);

    @Test
    void tokenRoundTripReturnsSameUserId() {
        String token = jwt.generate(7L, "a@b.com");
        assertEquals(7L, jwt.parseUserId(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.generate(7L, "a@b.com");
        assertThrows(JwtException.class, () -> jwt.parseUserId(token + "x"));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expired = new JwtService("unit-test-secret-0123456789-abcdefghij", -1000);
        assertThrows(JwtException.class, () -> jwt.parseUserId(expired.generate(1L, "a@b.com")));
    }
}
