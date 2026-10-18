package com.viernes.leads.application.service;

import com.viernes.leads.application.dto.LeadRequest;
import com.viernes.leads.application.dto.LeadResponse;
import com.viernes.leads.application.port.out.LeadPersistencePort;
import com.viernes.leads.domain.EstadoLead;
import com.viernes.leads.domain.Lead;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadPersistencePort persistencePort;

    @InjectMocks
    private LeadService servicio;

    private LeadRequest validRequest;
    private Lead entidadGuardada;

    @BeforeEach
    void setUp() {
        validRequest = new LeadRequest(
                "Ana", "Empresa SA", "ana@mail.com", "999888777", "WEB", null, "usr1", LocalDate.now()
        );

        entidadGuardada = Lead.builder()
                .id(1L)
                .nombre("Ana")
                .empresa("Empresa SA")
                .correo("ana@mail.com")
                .telefono("999888777")
                .fuente("WEB")
                .estado(EstadoLead.NUEVO)
                .responsable("usr1")
                .fecha(LocalDate.now())
                .creadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void crear_sinEstado_asignaNuevoPorDefecto() {
        when(persistencePort.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));

        LeadResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals(EstadoLead.NUEVO, response.estado());
        assertEquals("Ana", response.nombre());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }

    @Test
    void actualizar_existente_retornaActualizado() {
        LeadRequest cambios = new LeadRequest(
                "Ana Maria", "Empresa SA", "ana@mail.com", "999888777", "WEB",
                EstadoLead.CONTACTADO, "usr1", LocalDate.now()
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));

        LeadResponse response = servicio.actualizar(1L, cambios);

        assertNotNull(response);
        assertEquals("Ana Maria", response.nombre());
        assertEquals(EstadoLead.CONTACTADO, response.estado());
        verify(persistencePort).save(any(Lead.class));
    }

    @Test
    void cambiarEstado_existente_cambiaEstado() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Lead.class))).thenAnswer(i -> i.getArgument(0));

        LeadResponse response = servicio.cambiarEstado(1L, EstadoLead.CALIFICADO);

        assertEquals(EstadoLead.CALIFICADO, response.estado());
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
