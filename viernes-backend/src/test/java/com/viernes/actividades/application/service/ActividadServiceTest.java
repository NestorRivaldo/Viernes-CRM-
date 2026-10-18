package com.viernes.actividades.application.service;

import com.viernes.actividades.application.dto.ActividadRequest;
import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.application.port.out.ActividadPersistencePort;
import com.viernes.actividades.application.port.out.RelacionValidationPort;
import com.viernes.actividades.domain.Actividad;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.actividades.domain.TipoActividad;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActividadServiceTest {

    @Mock
    private ActividadPersistencePort persistencePort;

    @Mock
    private RelacionValidationPort validationPort;

    @InjectMocks
    private ActividadService servicio;

    @BeforeEach
    void setUp() {
    }

    @Test
    void crearActividadExitosamente() {
        ActividadRequest request = new ActividadRequest(
                "usr1", 1L, null, null, TipoActividad.REUNION, "Titulo", "Desc", LocalDateTime.now().plusDays(1), null
        );

        when(validationPort.existeCliente(1L)).thenReturn(true);
        when(validationPort.existeUsuario("usr1")).thenReturn(true);
        
        Actividad savedEntity = new Actividad(
                1L, "usr1", 1L, null, null, TipoActividad.REUNION, "Titulo", "Desc", request.fecha(), EstadoActividad.PENDIENTE, LocalDateTime.now()
        );
        when(persistencePort.save(any(Actividad.class))).thenReturn(savedEntity);

        ActividadResponse response = servicio.crear(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(EstadoActividad.PENDIENTE, response.estado());
        assertFalse(response.vencida());
    }

    @Test
    void crearActividadFallaPorClienteInexistente() {
        ActividadRequest request = new ActividadRequest(
                null, 99L, null, null, TipoActividad.REUNION, "Titulo", "Desc", LocalDateTime.now(), null
        );

        when(validationPort.existeCliente(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> servicio.crear(request));
    }

    @Test
    void verificarActividadVencida() {
        // Actividad con estado PENDIENTE y fecha en el pasado
        Actividad entidadGuardada = new Actividad(
                1L, null, null, null, null, TipoActividad.TAREA, "Hacer reporte", "", LocalDateTime.now().minusDays(2), EstadoActividad.PENDIENTE, LocalDateTime.now()
        );
        
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        ActividadResponse response = servicio.obtener(1L);

        assertTrue(response.vencida(), "La actividad debería estar vencida");
    }

    @Test
    void verificarActividadNoVencidaSiCompletada() {
        // Actividad con estado COMPLETADA y fecha en el pasado
        Actividad entidadGuardada = new Actividad(
                1L, null, null, null, null, TipoActividad.TAREA, "Hacer reporte", "", LocalDateTime.now().minusDays(2), EstadoActividad.COMPLETADA, LocalDateTime.now()
        );
        
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        ActividadResponse response = servicio.obtener(1L);

        assertFalse(response.vencida(), "La actividad no debería estar vencida si está completada");
    }

    @Test
    void actualizarActividadFallaSiNoExiste() {
        ActividadRequest request = new ActividadRequest(
                null, null, null, null, TipoActividad.REUNION, "Titulo", "Desc", LocalDateTime.now(), null
        );

        when(persistencePort.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.actualizar(1L, request));
    }
}
