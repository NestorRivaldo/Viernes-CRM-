package com.viernes.usuarios.application.service;

import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.application.dto.UsuarioRequest;
import com.viernes.usuarios.application.dto.UsuarioResponse;
import com.viernes.usuarios.application.port.out.UsuarioPersistencePort;
import com.viernes.usuarios.domain.EstadoUsuario;
import com.viernes.usuarios.domain.RolUsuario;
import com.viernes.usuarios.domain.Usuario;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioPersistencePort persistencePort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService servicio;

    private UsuarioRequest validRequest;
    private Usuario entidadGuardada;

    @BeforeEach
    void setUp() {
        validRequest = new UsuarioRequest(
                "Juan", "Pérez", "juan.perez@example.com", "secreto123", RolUsuario.VENDEDOR
        );

        entidadGuardada = Usuario.builder()
                .id(1L)
                .nombre("Juan")
                .apellido("Pérez")
                .correo("juan.perez@example.com")
                .passwordHash("hash-secreto")
                .rol(RolUsuario.VENDEDOR)
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void crear_conDatosValidos_codificaPasswordYFuerzaActivo() {
        when(persistencePort.existsByCorreo(validRequest.correo())).thenReturn(false);
        when(passwordEncoder.encode("secreto123")).thenReturn("hash-encriptado");
        when(persistencePort.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals("juan.perez@example.com", response.correo());
        assertEquals(EstadoUsuario.ACTIVO, response.estado());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(persistencePort).save(captor.capture());
        assertEquals("hash-encriptado", captor.getValue().getPasswordHash());
        assertEquals(EstadoUsuario.ACTIVO, captor.getValue().getEstado());
        verify(passwordEncoder).encode("secreto123");
    }

    @Test
    void crear_sinPassword_lanzaIllegalArgument() {
        UsuarioRequest sinPassword = new UsuarioRequest(
                "Juan", "Pérez", "juan.perez@example.com", null, RolUsuario.VENDEDOR
        );

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(sinPassword));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void crear_conCorreoDuplicado_lanzaIllegalArgument() {
        when(persistencePort.existsByCorreo(validRequest.correo())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void obtener_idInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }

    @Test
    void actualizar_conDatosValidos_retornaResponseActualizado() {
        UsuarioRequest updateRequest = new UsuarioRequest(
                "Carlos", "Pérez", "juan.perez@example.com", null, RolUsuario.JEFE
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        UsuarioResponse response = servicio.actualizar(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Carlos", response.nombre());
        assertEquals(RolUsuario.JEFE, response.rol());
        verify(persistencePort).save(any(Usuario.class));
    }

    @Test
    void actualizar_idInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.actualizar(99L, validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void activar_usuarioInactivo_cambiaEstadoAActivo() {
        entidadGuardada.setEstado(EstadoUsuario.INACTIVO);
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        servicio.activar(1L);

        assertEquals(EstadoUsuario.ACTIVO, entidadGuardada.getEstado());
        verify(persistencePort).save(entidadGuardada);
    }

    @Test
    void desactivar_usuarioActivo_cambiaEstadoAInactivo() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        servicio.desactivar(1L);

        assertEquals(EstadoUsuario.INACTIVO, entidadGuardada.getEstado());
        verify(persistencePort).save(entidadGuardada);
    }

    @Test
    void buscarPorCorreo_usuarioExistente_devuelveResumenConRolYEstadoComoString() {
        when(persistencePort.findByCorreo("juan.perez@example.com")).thenReturn(Optional.of(entidadGuardada));

        Optional<UsuarioResumen> resumen = servicio.buscarPorCorreo("juan.perez@example.com");

        assertTrue(resumen.isPresent());
        assertEquals(1L, resumen.get().id());
        assertEquals("juan.perez@example.com", resumen.get().correo());
        assertEquals("hash-secreto", resumen.get().passwordHash());
        assertEquals("VENDEDOR", resumen.get().rol());
        assertEquals("ACTIVO", resumen.get().estado());
    }

    @Test
    void buscarPorCorreo_inexistente_devuelveVacio() {
        when(persistencePort.findByCorreo("nadie@example.com")).thenReturn(Optional.empty());

        assertTrue(servicio.buscarPorCorreo("nadie@example.com").isEmpty());
    }
}
