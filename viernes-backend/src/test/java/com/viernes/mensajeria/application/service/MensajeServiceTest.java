package com.viernes.mensajeria.application.service;

import com.viernes.mensajeria.application.dto.MensajeRequest;
import com.viernes.mensajeria.application.dto.MensajeResponse;
import com.viernes.mensajeria.application.dto.ResumenMensajesResponse;
import com.viernes.mensajeria.application.port.out.MensajePersistencePort;
import com.viernes.mensajeria.domain.Mensaje;
import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.UsuariosApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MensajeServiceTest {

    @Mock
    private MensajePersistencePort persistencePort;

    @Mock
    private UsuariosApi usuariosApi;

    @InjectMocks
    private MensajeService servicio;

    private final LocalDateTime ahora = LocalDateTime.now();

    @Test
    void listar_sinDespuesDeId_retornaRecientes() {
        Mensaje mensaje = Mensaje.builder()
                .id(1L)
                .usuarioCorreo("admin@viernes.com")
                .usuarioNombre("Admin Viernes")
                .contenido("Hola equipo")
                .creadoEn(ahora)
                .build();
        when(persistencePort.findRecientes(50)).thenReturn(List.of(mensaje));

        List<MensajeResponse> resultado = servicio.listar(null, 50);

        assertEquals(1, resultado.size());
        assertEquals("Hola equipo", resultado.get(0).contenido());
        verify(persistencePort, never()).findDespuesDeId(any(), anyInt());
    }

    @Test
    void listar_conDespuesDeId_usaConsultaIncremental() {
        when(persistencePort.findDespuesDeId(5L, 50)).thenReturn(List.of());

        List<MensajeResponse> resultado = servicio.listar(5L, 50);

        assertTrue(resultado.isEmpty());
        verify(persistencePort).findDespuesDeId(5L, 50);
    }

    @Test
    void resumen_retornaTotalYUltimoId() {
        when(persistencePort.countDespuesDeId(5L)).thenReturn(3L);
        when(persistencePort.findUltimoId()).thenReturn(8L);

        ResumenMensajesResponse resultado = servicio.resumen(5L);

        assertEquals(3L, resultado.total());
        assertEquals(8L, resultado.ultimoId());
    }

    @Test
    void enviar_conUsuarioConocido_guardaNombreCompleto() {
        when(usuariosApi.buscarPorCorreo("admin@viernes.com")).thenReturn(Optional.of(
                new UsuarioResumen(1L, "Admin", "Viernes", "admin@viernes.com",
                        "hash", "ADMIN", "ACTIVO", ahora)
        ));
        when(persistencePort.save(any(Mensaje.class))).thenAnswer(invocation -> {
            Mensaje mensaje = invocation.getArgument(0);
            mensaje.setId(10L);
            return mensaje;
        });

        MensajeResponse resultado = servicio.enviar(
                "admin@viernes.com",
                null,
                new MensajeRequest("  Hola equipo  ")
        );

        assertEquals(10L, resultado.id());
        assertEquals("Admin Viernes", resultado.usuarioNombre());
        assertEquals("Hola equipo", resultado.contenido());
        assertEquals("admin@viernes.com", resultado.usuarioCorreo());
    }

    @Test
    void enviar_conNombrePreferido_usaNombreDelToken() {
        when(usuariosApi.buscarPorCorreo("admin@viernes.com")).thenReturn(Optional.of(
                new UsuarioResumen(1L, "Admin", "Viernes", "admin@viernes.com",
                        "hash", "ADMIN", "ACTIVO", ahora)
        ));
        when(persistencePort.save(any(Mensaje.class))).thenAnswer(invocation -> {
            Mensaje mensaje = invocation.getArgument(0);
            mensaje.setId(11L);
            return mensaje;
        });

        MensajeResponse resultado = servicio.enviar(
                "admin@viernes.com",
                "Nestor Barrantes",
                new MensajeRequest("Hola equipo")
        );

        assertEquals("Nestor Barrantes", resultado.usuarioNombre());
    }

    @Test
    void enviar_contenidoEnBlanco_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> servicio.enviar("admin@viernes.com", null, new MensajeRequest("   ")));

        assertEquals("El mensaje no puede estar vacío", ex.getMessage());
        verify(persistencePort, never()).save(any());
    }
}
