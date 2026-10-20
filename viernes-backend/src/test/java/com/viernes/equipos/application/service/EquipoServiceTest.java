package com.viernes.equipos.application.service;

import com.viernes.equipos.application.dto.EquipoRequest;
import com.viernes.equipos.application.dto.EquipoResponse;
import com.viernes.equipos.application.dto.MiembroResponse;
import com.viernes.equipos.application.port.out.EquipoMiembroPersistencePort;
import com.viernes.equipos.application.port.out.EquipoPersistencePort;
import com.viernes.equipos.application.port.out.UsuarioValidationPort;
import com.viernes.equipos.domain.Equipo;
import com.viernes.equipos.domain.EquipoMiembro;
import com.viernes.equipos.domain.EstadoEquipo;
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
class EquipoServiceTest {

    @Mock
    private EquipoPersistencePort persistencePort;

    @Mock
    private EquipoMiembroPersistencePort miembroPersistencePort;

    @Mock
    private UsuarioValidationPort usuarioValidationPort;

    @InjectMocks
    private EquipoService servicio;

    private EquipoRequest validRequest;
    private Equipo equipoGuardado;

    @BeforeEach
    void setUp() {
        validRequest = new EquipoRequest("Equipo Norte", "Cobertura zona norte");

        equipoGuardado = Equipo.builder()
                .id(1L)
                .nombre("Equipo Norte")
                .descripcion("Cobertura zona norte")
                .estado(EstadoEquipo.ACTIVO)
                .creadoEn(LocalDateTime.now())
                .build();
    }

    @Test
    void crear_nombreDisponible_creaEquipoActivo() {
        when(persistencePort.existsByNombre("Equipo Norte")).thenReturn(false);
        when(persistencePort.save(any(Equipo.class))).thenAnswer(i -> {
            Equipo e = i.getArgument(0);
            e.setId(1L);
            return e;
        });

        EquipoResponse response = servicio.crear(validRequest);

        assertNotNull(response);
        assertEquals("Equipo Norte", response.nombre());
        assertTrue(response.activo());
        verify(persistencePort).save(any(Equipo.class));
    }

    @Test
    void crear_nombreDuplicado_lanzaIllegalArgument() {
        when(persistencePort.existsByNombre("Equipo Norte")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.crear(validRequest));
        verify(persistencePort, never()).save(any());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.obtener(99L));
    }

    @Test
    void agregarMiembro_datosValidos_agregaCorrectamente() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));
        when(usuarioValidationPort.existeUsuario(10L)).thenReturn(true);
        when(miembroPersistencePort.existsByEquipoIdAndUsuarioId(1L, 10L)).thenReturn(false);
        when(miembroPersistencePort.save(any(EquipoMiembro.class))).thenAnswer(i -> i.getArgument(0));

        MiembroResponse response = servicio.agregarMiembro(1L, 10L);

        assertNotNull(response);
        assertEquals(10L, response.usuarioId());
        assertNotNull(response.agregadoEn());
        verify(miembroPersistencePort).save(any(EquipoMiembro.class));
    }

    @Test
    void agregarMiembro_equipoInexistente_lanzaNotFound() {
        when(persistencePort.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.agregarMiembro(99L, 10L));
        verify(miembroPersistencePort, never()).save(any());
    }

    @Test
    void agregarMiembro_usuarioInexistente_lanzaNotFound() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));
        when(usuarioValidationPort.existeUsuario(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> servicio.agregarMiembro(1L, 99L));
        verify(miembroPersistencePort, never()).save(any());
    }

    @Test
    void agregarMiembro_duplicado_lanzaIllegalArgument() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));
        when(usuarioValidationPort.existeUsuario(10L)).thenReturn(true);
        when(miembroPersistencePort.existsByEquipoIdAndUsuarioId(1L, 10L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicio.agregarMiembro(1L, 10L));
        verify(miembroPersistencePort, never()).save(any());
    }

    @Test
    void quitarMiembro_existente_eliminaSoloLaAsociacion() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));
        when(miembroPersistencePort.existsByEquipoIdAndUsuarioId(1L, 10L)).thenReturn(true);

        servicio.quitarMiembro(1L, 10L);

        verify(miembroPersistencePort).deleteByEquipoIdAndUsuarioId(1L, 10L);
    }

    @Test
    void quitarMiembro_asociacionInexistente_lanzaNotFound() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));
        when(miembroPersistencePort.existsByEquipoIdAndUsuarioId(1L, 10L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> servicio.quitarMiembro(1L, 10L));
        verify(miembroPersistencePort, never()).deleteByEquipoIdAndUsuarioId(any(), any());
    }

    @Test
    void listarMiembros_equipoExistente_retornaMiembros() {
        EquipoMiembro miembro = EquipoMiembro.builder()
                .id(5L).equipoId(1L).usuarioId(10L).creadoEn(LocalDateTime.now()).build();
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));
        when(miembroPersistencePort.findByEquipoId(1L)).thenReturn(List.of(miembro));

        List<MiembroResponse> miembros = servicio.listarMiembros(1L);

        assertEquals(1, miembros.size());
        assertEquals(10L, miembros.get(0).usuarioId());
    }

    @Test
    void eliminar_existente_eliminaMiembrosYEquipo() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));

        servicio.eliminar(1L);

        verify(miembroPersistencePort).deleteByEquipoId(1L);
        verify(persistencePort).deleteById(1L);
    }

    @Test
    void desactivar_existente_marcaInactivo() {
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));
        when(persistencePort.save(any(Equipo.class))).thenAnswer(i -> i.getArgument(0));

        servicio.desactivar(1L);

        assertEquals(EstadoEquipo.INACTIVO, equipoGuardado.getEstado());
        verify(persistencePort).save(equipoGuardado);
    }

    @Test
    void activar_existente_marcaActivo() {
        equipoGuardado.setEstado(EstadoEquipo.INACTIVO);
        when(persistencePort.findById(1L)).thenReturn(Optional.of(equipoGuardado));
        when(persistencePort.save(any(Equipo.class))).thenAnswer(i -> i.getArgument(0));

        servicio.activar(1L);

        assertEquals(EstadoEquipo.ACTIVO, equipoGuardado.getEstado());
        verify(persistencePort).save(equipoGuardado);
    }
}
