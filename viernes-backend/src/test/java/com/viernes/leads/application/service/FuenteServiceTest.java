package com.viernes.leads.application.service;

import com.viernes.leads.application.dto.FuenteRequest;
import com.viernes.leads.application.dto.FuenteResponse;
import com.viernes.leads.application.port.out.FuentePersistencePort;
import com.viernes.leads.domain.Fuente;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
class FuenteServiceTest {

    @Mock
    private FuentePersistencePort persistencePort;

    @InjectMocks
    private FuenteService servicio;

    private FuenteRequest validRequest;
    private Fuente entidadGuardada;

    @BeforeEach
    void setUp() {
        validRequest = new FuenteRequest("WEB", "Sitio Web", "Formulario web", null);

        entidadGuardada = Fuente.builder()
                .id(1L)
                .codigo("WEB")
                .nombre("Sitio Web")
                .descripcion("Formulario web")
                .activa(true)
                .creadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void listar_sinFiltro_retornaTodas() {
        when(persistencePort.findAll()).thenReturn(List.of(entidadGuardada));

        List<FuenteResponse> resultado = servicio.listar(null);

        assertEquals(1, resultado.size());
        verify(persistencePort).findAll();
        verify(persistencePort, never()).findByActiva(any());
    }

    @Test
    void listar_conActiva_filtraPorEstado() {
        when(persistencePort.findByActiva(true)).thenReturn(List.of(entidadGuardada));

        List<FuenteResponse> resultado = servicio.listar(true);

        assertEquals(1, resultado.size());
        verify(persistencePort).findByActiva(true);
        verify(persistencePort, never()).findAll();
    }

    @Test
    void crear_sinActiva_asignaActivaPorDefecto() {
        when(persistencePort.existsByCodigo("WEB")).thenReturn(false);
        when(persistencePort.save(any(Fuente.class))).thenAnswer(i -> i.getArgument(0));

        FuenteResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals("WEB", response.codigo());
        assertTrue(response.activa());
    }

    @Test
    void crear_codigoDuplicado_lanzaIllegalArgument() {
        when(persistencePort.existsByCodigo("WEB")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }

    @Test
    void actualizar_existente_retornaActualizado() {
        FuenteRequest cambios = new FuenteRequest("WEB", "Sitio Web Corporativo", "Nueva desc", false);
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Fuente.class))).thenAnswer(i -> i.getArgument(0));

        FuenteResponse response = servicio.actualizar(1L, cambios);

        assertNotNull(response);
        assertEquals("Sitio Web Corporativo", response.nombre());
        assertFalse(response.activa());
        verify(persistencePort).save(any(Fuente.class));
    }

    @Test
    void eliminar_existente_eliminaCorrectamente() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        servicio.eliminar(1L);

        verify(persistencePort).deleteById(1L);
    }

    @Test
    void eliminar_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.eliminar(99L));
        verify(persistencePort, never()).deleteById(any());
    }
}
