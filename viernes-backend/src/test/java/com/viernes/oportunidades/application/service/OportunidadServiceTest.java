package com.viernes.oportunidades.application.service;

import com.viernes.oportunidades.application.dto.OportunidadRequest;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.application.port.out.OportunidadPersistencePort;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import com.viernes.oportunidades.domain.Oportunidad;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OportunidadServiceTest {

    @Mock
    private OportunidadPersistencePort persistencePort;

    @InjectMocks
    private OportunidadService servicio;

    private OportunidadRequest validRequest;
    private Oportunidad entidadGuardada;

    @BeforeEach
    void setUp() {
        validRequest = new OportunidadRequest(
                "Venta ERP", "Cliente XYZ", new BigDecimal("10000.00"), 50,
                null, "Juan", LocalDate.now().plusDays(30)
        );

        entidadGuardada = Oportunidad.builder()
                .id(1L)
                .nombre("Venta ERP")
                .clienteNombre("Cliente XYZ")
                .valor(new BigDecimal("10000.00"))
                .probabilidad(50)
                .etapa(EtapaOportunidad.PROSPECCION)
                .responsable("Juan")
                .fechaCierre(LocalDate.now().plusDays(30))
                .creadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void crear_sinEtapa_asignaProspeccionPorDefecto() {
        when(persistencePort.save(any(Oportunidad.class))).thenAnswer(i -> i.getArgument(0));

        OportunidadResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals(EtapaOportunidad.PROSPECCION, response.etapa());
        assertEquals("Venta ERP", response.nombre());
        assertEquals(new BigDecimal("5000.00"), response.valorPonderado());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }

    @Test
    void actualizar_existente_actualizaCampos() {
        OportunidadRequest updateRequest = new OportunidadRequest(
                "Venta ERP Actualizada", "Cliente ABC", new BigDecimal("20000.00"), 75,
                EtapaOportunidad.NEGOCIACION, "Maria", LocalDate.now().plusDays(60)
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Oportunidad.class))).thenAnswer(i -> i.getArgument(0));

        OportunidadResponse response = servicio.actualizar(1L, updateRequest);

        assertEquals("Venta ERP Actualizada", response.nombre());
        assertEquals("Cliente ABC", response.clienteNombre());
        assertEquals(EtapaOportunidad.NEGOCIACION, response.etapa());
        verify(persistencePort).save(any(Oportunidad.class));
    }

    @Test
    void cambiarEtapa_existente_cambiaEtapa() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Oportunidad.class))).thenAnswer(i -> i.getArgument(0));

        OportunidadResponse response = servicio.cambiarEtapa(1L, EtapaOportunidad.GANADA);

        assertEquals(EtapaOportunidad.GANADA, response.etapa());
    }

    @Test
    void eliminar_existente_eliminaCorrectamente() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        servicio.eliminar(1L);

        verify(persistencePort).deleteById(1L);
    }
}
