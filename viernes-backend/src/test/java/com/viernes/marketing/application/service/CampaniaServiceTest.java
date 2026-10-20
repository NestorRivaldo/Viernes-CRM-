package com.viernes.marketing.application.service;

import com.viernes.leads.application.dto.LeadResponse;
import com.viernes.leads.application.port.in.ManageLeadUseCase;
import com.viernes.leads.domain.EstadoLead;
import com.viernes.marketing.application.dto.CampaniaLeadResponse;
import com.viernes.marketing.application.dto.CampaniaRequest;
import com.viernes.marketing.application.dto.CampaniaResponse;
import com.viernes.marketing.application.port.out.CampaniaLeadPersistencePort;
import com.viernes.marketing.application.port.out.CampaniaPersistencePort;
import com.viernes.marketing.domain.Campania;
import com.viernes.marketing.domain.CampaniaLead;
import com.viernes.marketing.domain.EstadoCampania;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaniaServiceTest {

    @Mock
    private CampaniaPersistencePort persistencePort;

    @Mock
    private CampaniaLeadPersistencePort campaniaLeadPort;

    @Mock
    private ManageLeadUseCase leadUseCase;

    @InjectMocks
    private CampaniaService servicio;

    private CampaniaRequest validRequest;
    private Campania entidadGuardada;
    private LeadResponse leadResponse;

    @BeforeEach
    void setUp() {
        validRequest = new CampaniaRequest(
                "Lanzamiento Q4", "Campania de lanzamiento", LocalDate.now(), LocalDate.now().plusDays(30), null
        );

        entidadGuardada = Campania.builder()
                .id(1L)
                .nombre("Lanzamiento Q4")
                .descripcion("Campania de lanzamiento")
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusDays(30))
                .estado(EstadoCampania.BORRADOR)
                .creadoEn(LocalDateTime.now())
                .build();

        leadResponse = new LeadResponse(
                7L, "Ana", "Empresa SA", "ana@mail.com", "999888777", "WEB",
                EstadoLead.NUEVO, "usr1", LocalDate.now(), LocalDateTime.now(), null, null, null
        );
    }

    @Test
    void crear_sinEstado_asignaBorradorPorDefecto() {
        when(persistencePort.save(any(Campania.class))).thenAnswer(i -> i.getArgument(0));

        CampaniaResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals(EstadoCampania.BORRADOR, response.estado());
        assertEquals("Lanzamiento Q4", response.nombre());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }

    @Test
    void actualizar_existente_retornaActualizado() {
        CampaniaRequest cambios = new CampaniaRequest(
                "Lanzamiento Q4 Plus", "Desc", LocalDate.now(), LocalDate.now().plusDays(60), EstadoCampania.ACTIVA
        );
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Campania.class))).thenAnswer(i -> i.getArgument(0));

        CampaniaResponse response = servicio.actualizar(1L, cambios);

        assertNotNull(response);
        assertEquals("Lanzamiento Q4 Plus", response.nombre());
        assertEquals(EstadoCampania.ACTIVA, response.estado());
        verify(persistencePort).save(any(Campania.class));
    }

    @Test
    void cambiarEstado_existente_cambiaEstado() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(persistencePort.save(any(Campania.class))).thenAnswer(i -> i.getArgument(0));

        CampaniaResponse response = servicio.cambiarEstado(1L, EstadoCampania.FINALIZADA);

        assertEquals(EstadoCampania.FINALIZADA, response.estado());
    }

    @Test
    void eliminar_existente_eliminaAsociacionesYCampania() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));

        servicio.eliminar(1L);

        verify(campaniaLeadPort).deleteByCampaniaId(1L);
        verify(persistencePort).deleteById(1L);
    }

    @Test
    void eliminar_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.eliminar(99L));
        verify(campaniaLeadPort, never()).deleteByCampaniaId(any());
        verify(persistencePort, never()).deleteById(any());
    }

    @Test
    void asociarLead_valido_guardaYRetornaEnriquecido() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(leadUseCase.obtener(7L)).thenReturn(leadResponse);
        when(campaniaLeadPort.existsByCampaniaIdAndLeadId(1L, 7L)).thenReturn(false);
        when(campaniaLeadPort.save(any(CampaniaLead.class))).thenAnswer(i -> i.getArgument(0));

        CampaniaLeadResponse response = servicio.asociarLead(1L, 7L);

        assertNotNull(response);
        assertEquals(1L, response.campaniaId());
        assertEquals(7L, response.leadId());
        assertEquals("Ana", response.leadNombre());
        assertEquals("Empresa SA", response.leadEmpresa());
        verify(campaniaLeadPort).save(any(CampaniaLead.class));
    }

    @Test
    void asociarLead_campaniaInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.asociarLead(99L, 7L));
        verify(campaniaLeadPort, never()).save(any());
    }

    @Test
    void asociarLead_leadInexistente_lanzaNotFound() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(leadUseCase.obtener(99L)).thenThrow(new ResourceNotFoundException("Lead no encontrado: 99"));

        assertThrows(ResourceNotFoundException.class, () -> servicio.asociarLead(1L, 99L));
        verify(campaniaLeadPort, never()).save(any());
    }

    @Test
    void asociarLead_duplicado_lanzaIllegalArgument() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(leadUseCase.obtener(7L)).thenReturn(leadResponse);
        when(campaniaLeadPort.existsByCampaniaIdAndLeadId(1L, 7L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.asociarLead(1L, 7L));
        verify(campaniaLeadPort, never()).save(any());
    }

    @Test
    void desasociarLead_existente_eliminaAsociacion() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(campaniaLeadPort.existsByCampaniaIdAndLeadId(1L, 7L)).thenReturn(true);

        servicio.desasociarLead(1L, 7L);

        verify(campaniaLeadPort).deleteByCampaniaIdAndLeadId(1L, 7L);
    }

    @Test
    void desasociarLead_inexistente_lanzaNotFound() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(campaniaLeadPort.existsByCampaniaIdAndLeadId(1L, 7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> servicio.desasociarLead(1L, 7L));
        verify(campaniaLeadPort, never()).deleteByCampaniaIdAndLeadId(any(), any());
    }

    @Test
    void listarLeads_retornaAsociacionesEnriquecidas() {
        CampaniaLead asociacion = CampaniaLead.builder()
                .id(5L).campaniaId(1L).leadId(7L).creadoEn(LocalDateTime.now())
                .build();
        when(persistencePort.findById(1L)).thenReturn(Optional.of(entidadGuardada));
        when(campaniaLeadPort.findByCampaniaId(1L)).thenReturn(List.of(asociacion));
        when(leadUseCase.obtener(7L)).thenReturn(leadResponse);

        List<CampaniaLeadResponse> resultado = servicio.listarLeads(1L);

        assertEquals(1, resultado.size());
        assertEquals(7L, resultado.get(0).leadId());
        assertEquals("Ana", resultado.get(0).leadNombre());
        assertEquals(EstadoLead.NUEVO, resultado.get(0).leadEstado());
    }
}
