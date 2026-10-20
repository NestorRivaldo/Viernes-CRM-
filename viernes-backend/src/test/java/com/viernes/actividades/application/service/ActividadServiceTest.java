package com.viernes.actividades.application.service;

import com.viernes.actividades.application.dto.ActividadRequest;
import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.application.dto.ParticipanteResponse;
import com.viernes.actividades.application.port.out.ActividadParticipantePersistencePort;
import com.viernes.actividades.application.port.out.ActividadPersistencePort;
import com.viernes.actividades.application.port.out.ContactoQueryPort;
import com.viernes.actividades.application.port.out.RelacionValidationPort;
import com.viernes.actividades.domain.Actividad;
import com.viernes.actividades.domain.ActividadParticipante;
import com.viernes.actividades.domain.EstadoActividad;
import com.viernes.actividades.domain.TipoActividad;
import com.viernes.clientes.application.dto.ContactoResponse;
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
class ActividadServiceTest {

    @Mock
    private ActividadPersistencePort persistencePort;

    @Mock
    private RelacionValidationPort validationPort;

    @Mock
    private ActividadParticipantePersistencePort participantePort;

    @Mock
    private ContactoQueryPort contactoQueryPort;

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

    @Test
    void agregarParticipanteExitosamente() {
        Actividad actividad = new Actividad(
                1L, "usr1", 1L, null, null, TipoActividad.REUNION, "Titulo", "Desc",
                LocalDateTime.now().plusDays(1), EstadoActividad.PENDIENTE, LocalDateTime.now()
        );
        ContactoResponse contacto = new ContactoResponse(
                5L, 1L, "Ana", "Perez", "Gerente", "ana@mail.com", "555-1234", true, LocalDateTime.now()
        );
        ActividadParticipante guardado = new ActividadParticipante(10L, 1L, 5L, LocalDateTime.now());

        when(persistencePort.findById(1L)).thenReturn(Optional.of(actividad));
        when(contactoQueryPort.buscarPorId(5L)).thenReturn(Optional.of(contacto));
        when(participantePort.existsByIdActividadAndIdContacto(1L, 5L)).thenReturn(false);
        when(participantePort.save(any(ActividadParticipante.class))).thenReturn(guardado);

        ParticipanteResponse response = servicio.agregarParticipante(1L, 5L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.idActividad());
        assertEquals(5L, response.contactoId());
        assertEquals("Ana Perez", response.nombreContacto());
        assertEquals("ana@mail.com", response.correoContacto());
    }

    @Test
    void agregarParticipanteFallaPorActividadInexistente() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.agregarParticipante(99L, 5L));
        verify(participantePort, never()).save(any(ActividadParticipante.class));
    }

    @Test
    void agregarParticipanteFallaPorContactoInexistente() {
        Actividad actividad = new Actividad(
                1L, "usr1", null, null, null, TipoActividad.REUNION, "Titulo", "Desc",
                LocalDateTime.now().plusDays(1), EstadoActividad.PENDIENTE, LocalDateTime.now()
        );

        when(persistencePort.findById(1L)).thenReturn(Optional.of(actividad));
        when(contactoQueryPort.buscarPorId(77L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.agregarParticipante(1L, 77L));
        verify(participantePort, never()).save(any(ActividadParticipante.class));
    }

    @Test
    void agregarParticipanteFallaPorDuplicado() {
        Actividad actividad = new Actividad(
                1L, "usr1", null, null, null, TipoActividad.REUNION, "Titulo", "Desc",
                LocalDateTime.now().plusDays(1), EstadoActividad.PENDIENTE, LocalDateTime.now()
        );
        ContactoResponse contacto = new ContactoResponse(
                5L, 1L, "Ana", "Perez", "Gerente", "ana@mail.com", "555-1234", true, LocalDateTime.now()
        );

        when(persistencePort.findById(1L)).thenReturn(Optional.of(actividad));
        when(contactoQueryPort.buscarPorId(5L)).thenReturn(Optional.of(contacto));
        when(participantePort.existsByIdActividadAndIdContacto(1L, 5L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.agregarParticipante(1L, 5L));
        verify(participantePort, never()).save(any(ActividadParticipante.class));
    }

    @Test
    void quitarParticipanteExitosamente() {
        ActividadParticipante asociacion = new ActividadParticipante(10L, 1L, 5L, LocalDateTime.now());

        when(participantePort.findByIdActividadAndIdContacto(1L, 5L)).thenReturn(Optional.of(asociacion));

        servicio.quitarParticipante(1L, 5L);

        verify(participantePort).delete(asociacion);
    }

    @Test
    void quitarParticipanteFallaSiNoExisteLaAsociacion() {
        when(participantePort.findByIdActividadAndIdContacto(1L, 5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.quitarParticipante(1L, 5L));
        verify(participantePort, never()).delete(any(ActividadParticipante.class));
    }

    @Test
    void listarParticipantesEnriquecidosConDatosDelContacto() {
        Actividad actividad = new Actividad(
                1L, "usr1", null, null, null, TipoActividad.REUNION, "Titulo", "Desc",
                LocalDateTime.now().plusDays(1), EstadoActividad.PENDIENTE, LocalDateTime.now()
        );
        ActividadParticipante participante = new ActividadParticipante(10L, 1L, 5L, LocalDateTime.now());
        ContactoResponse contacto = new ContactoResponse(
                5L, 1L, "Ana", "Perez", "Gerente", "ana@mail.com", "555-1234", true, LocalDateTime.now()
        );

        when(persistencePort.findById(1L)).thenReturn(Optional.of(actividad));
        when(participantePort.findByIdActividad(1L)).thenReturn(List.of(participante));
        when(contactoQueryPort.buscarPorId(5L)).thenReturn(Optional.of(contacto));

        List<ParticipanteResponse> participantes = servicio.listarParticipantes(1L);

        assertEquals(1, participantes.size());
        assertEquals(5L, participantes.get(0).contactoId());
        assertEquals("Ana Perez", participantes.get(0).nombreContacto());
        assertEquals("ana@mail.com", participantes.get(0).correoContacto());
    }

    @Test
    void listarParticipantesFallaPorActividadInexistente() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.listarParticipantes(99L));
    }

    @Test
    void eliminarActividadEliminaSusParticipantes() {
        Actividad actividad = new Actividad(
                1L, "usr1", null, null, null, TipoActividad.TAREA, "Titulo", "Desc",
                LocalDateTime.now().plusDays(1), EstadoActividad.PENDIENTE, LocalDateTime.now()
        );

        when(persistencePort.findById(1L)).thenReturn(Optional.of(actividad));

        servicio.eliminar(1L);

        verify(participantePort).deleteByIdActividad(1L);
        verify(persistencePort).deleteById(1L);
    }
}
