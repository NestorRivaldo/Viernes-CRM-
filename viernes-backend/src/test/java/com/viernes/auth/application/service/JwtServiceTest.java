package com.viernes.auth.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String secret = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes());
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);

        userDetails = User.builder()
                .username("juan@test.com")
                .password("hash123")
                .roles("ADMIN")
                .build();
    }

    @Test
    void generateToken_permiteExtraerUsername() {
        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertEquals("juan@test.com", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_conUsuarioCorrecto_retornaTrue() {
        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_conOtroUsername_retornaFalse() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otroUsuario = User.builder()
                .username("otro@test.com")
                .password("hash123")
                .roles("ADMIN")
                .build();

        assertFalse(jwtService.isTokenValid(token, otroUsuario));
    }

    @Test
    void isTokenValid_conTokenExpirado_retornaFalse() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken(userDetails);

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }
}
