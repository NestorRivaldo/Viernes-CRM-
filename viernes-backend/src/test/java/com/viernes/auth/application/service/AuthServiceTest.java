package com.viernes.auth.application.service;

import com.viernes.auth.application.dto.LoginRequest;
import com.viernes.auth.application.dto.LoginResponse;
import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.UsuariosApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuariosApi usuariosApi;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService servicio;

    private LoginRequest validRequest;
    private UsuarioResumen usuarioActivo;

    @BeforeEach
    void setUp() {
        validRequest = new LoginRequest("juan@test.com", "secreto123");

        usuarioActivo = new UsuarioResumen(
                1L, "Juan", "Pérez", "juan@test.com", "hash123",
                "ADMIN", "ACTIVO", LocalDateTime.now()
        );
    }

    @Test
    void login_conCredencialesValidas_devuelveTokenYDatosUsuario() {
        when(usuariosApi.buscarPorCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("secreto123", "hash123")).thenReturn(true);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token-jwt");
        when(jwtService.getExpiration()).thenReturn(3600000L);

        LoginResponse response = servicio.login(validRequest);

        assertNotNull(response);
        assertEquals("token-jwt", response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals(3600000L, response.expiresIn());
        assertEquals(1L, response.usuario().id());
        assertEquals("Juan", response.usuario().nombre());
        assertEquals("Pérez", response.usuario().apellido());
        assertEquals("juan@test.com", response.usuario().correo());
        assertEquals("ADMIN", response.usuario().rol());
        assertEquals("ACTIVO", response.usuario().estado());
    }

    @Test
    void login_conCorreoInexistente_lanzaBadCredentials() {
        when(usuariosApi.buscarPorCorreo("juan@test.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> servicio.login(validRequest));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    @Test
    void login_conPasswordIncorrecta_lanzaBadCredentials() {
        when(usuariosApi.buscarPorCorreo("juan@test.com")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("secreto123", "hash123")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> servicio.login(validRequest));
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }

    @Test
    void login_conUsuarioInactivo_lanzaDisabled() {
        UsuarioResumen usuarioInactivo = new UsuarioResumen(
                1L, "Juan", "Pérez", "juan@test.com", "hash123",
                "ADMIN", "INACTIVO", LocalDateTime.now()
        );
        when(usuariosApi.buscarPorCorreo("juan@test.com")).thenReturn(Optional.of(usuarioInactivo));
        when(passwordEncoder.matches("secreto123", "hash123")).thenReturn(true);

        assertThrows(DisabledException.class, () -> servicio.login(validRequest));
        verify(jwtService, never()).generateToken(any(UserDetails.class));
    }
}
