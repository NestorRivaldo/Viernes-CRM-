package com.viernes.etiquetas.application.service;

import com.viernes.etiquetas.application.dto.EtiquetaRequest;
import com.viernes.etiquetas.application.dto.EtiquetaResponse;
import com.viernes.etiquetas.application.port.out.EtiquetaPersistencePort;
import com.viernes.etiquetas.domain.Etiqueta;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class EtiquetaServiceTest {

    @Mock
    private EtiquetaPersistencePort persistencePort;

    @InjectMocks
    private EtiquetaService servicio;

    @Test
    void crearNormalizaNombreYColor() {
        EtiquetaRequest request = new EtiquetaRequest("  Cliente   VIP  ", "#2563eb", "  Cuenta clave  ", null);

        when(persistencePort.existsByNombreNormalizado("CLIENTE VIP")).thenReturn(false);
        when(persistencePort.save(any(Etiqueta.class))).thenAnswer(invocation -> {
            Etiqueta etiqueta = invocation.getArgument(0);
            etiqueta.setId(1L);
            return etiqueta;
        });

        EtiquetaResponse response = servicio.crear(request);

        assertEquals(1L, response.id());
        assertEquals("Cliente VIP", response.nombre());
        assertEquals("#2563EB", response.color());
        assertEquals("Cuenta clave", response.descripcion());
        assertTrue(response.activa());

        ArgumentCaptor<Etiqueta> captor = ArgumentCaptor.forClass(Etiqueta.class);
        verify(persistencePort).save(captor.capture());
        assertEquals("CLIENTE VIP", captor.getValue().getNombreNormalizado());
    }

    @Test
    void crearRechazaNombreDuplicado() {
        EtiquetaRequest request = new EtiquetaRequest("Cliente VIP", "#2563EB", null, true);
        when(persistencePort.existsByNombreNormalizado("CLIENTE VIP")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> servicio.crear(request));

        assertTrue(ex.getMessage().contains("Ya existe"));
        verify(persistencePort, never()).save(any(Etiqueta.class));
    }

    @Test
    void eliminarArchivaEnLugarDeBorrar() {
        Etiqueta etiqueta = Etiqueta.builder()
                .id(1L)
                .nombre("Cliente")
                .nombreNormalizado("CLIENTE")
                .color("#2563EB")
                .activa(true)
                .creadoEn(LocalDateTime.now())
                .build();

        when(persistencePort.findById(1L)).thenReturn(Optional.of(etiqueta));

        servicio.eliminar(1L);

        assertFalse(etiqueta.getActiva());
        verify(persistencePort).save(etiqueta);
    }

    @Test
    void listarFiltraPorTextoYOrdena() {
        Etiqueta cliente = Etiqueta.builder()
                .id(1L).nombre("Cliente").nombreNormalizado("CLIENTE").color("#2563EB").activa(true).build();
        Etiqueta clienteVip = Etiqueta.builder()
                .id(2L).nombre("Cliente VIP").nombreNormalizado("CLIENTE VIP").color("#7C3AED").activa(true).build();
        Etiqueta urgente = Etiqueta.builder()
                .id(3L).nombre("Urgente").nombreNormalizado("URGENTE").color("#DC2626").activa(true).build();

        when(persistencePort.findByActiva(true)).thenReturn(List.of(urgente, clienteVip, cliente));

        List<EtiquetaResponse> response = servicio.listar(true, "cliente");

        assertEquals(2, response.size());
        assertEquals("Cliente", response.get(0).nombre());
        assertEquals("Cliente VIP", response.get(1).nombre());
    }

    @Test
    void obtenerFallaSiNoExiste() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }
}
