package com.viernes.equipos.adapters.in.web;

import com.viernes.equipos.application.dto.EquipoRequest;
import com.viernes.equipos.application.dto.EquipoResponse;
import com.viernes.equipos.application.dto.MiembroResponse;
import com.viernes.equipos.application.port.in.ManageEquipoUseCase;
import com.viernes.equipos.domain.EstadoEquipo;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EquipoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageEquipoUseCase useCase;

    @InjectMocks
    private EquipoController controller;

    private EquipoResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();

        validResponse = new EquipoResponse(
                1L, "Equipo Norte", "Cobertura zona norte", true, LocalDateTime.now(), 3L
        );
    }

    @Test
    void listarEquipos() throws Exception {
        when(useCase.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/equipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listarEquipos_conParametroActivo_llamaListarPorEstado() throws Exception {
        when(useCase.listarPorEstado(EstadoEquipo.ACTIVO)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/equipos?activo=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(useCase).listarPorEstado(EstadoEquipo.ACTIVO);
    }

    @Test
    void obtenerEquipoExistente() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/equipos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Equipo Norte"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.totalMiembros").value(3));
    }

    @Test
    void obtenerEquipoInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/equipos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearEquipoValido() throws Exception {
        when(useCase.crear(any(EquipoRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/equipos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Equipo Norte\",\"descripcion\":\"Cobertura zona norte\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void crearEquipoInvalido_sinNombre() throws Exception {
        mockMvc.perform(post("/api/v1/equipos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"descripcion\":\"Sin nombre\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarEquipo() throws Exception {
        when(useCase.actualizar(eq(1L), any(EquipoRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/equipos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Equipo Norte\",\"descripcion\":\"Nueva descripcion\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void activarEquipo() throws Exception {
        mockMvc.perform(patch("/api/v1/equipos/1/activar"))
                .andExpect(status().isNoContent());

        verify(useCase).activar(1L);
    }

    @Test
    void desactivarEquipo() throws Exception {
        mockMvc.perform(patch("/api/v1/equipos/1/desactivar"))
                .andExpect(status().isNoContent());

        verify(useCase).desactivar(1L);
    }

    @Test
    void eliminarEquipo() throws Exception {
        mockMvc.perform(delete("/api/v1/equipos/1"))
                .andExpect(status().isNoContent());

        verify(useCase).eliminar(1L);
    }

    @Test
    void listarMiembros() throws Exception {
        MiembroResponse miembro = new MiembroResponse(10L, "Ana Torres", "ana@viernes.com", LocalDateTime.now());
        when(useCase.listarMiembros(1L)).thenReturn(List.of(miembro));

        mockMvc.perform(get("/api/v1/equipos/1/miembros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].usuarioId").value(10L));
    }

    @Test
    void agregarMiembro() throws Exception {
        MiembroResponse miembro = new MiembroResponse(10L, "Ana Torres", "ana@viernes.com", LocalDateTime.now());
        when(useCase.agregarMiembro(1L, 10L)).thenReturn(miembro);

        mockMvc.perform(post("/api/v1/equipos/1/miembros")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuarioId\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(10L));
    }

    @Test
    void quitarMiembro() throws Exception {
        mockMvc.perform(delete("/api/v1/equipos/1/miembros/10"))
                .andExpect(status().isNoContent());

        verify(useCase).quitarMiembro(1L, 10L);
    }
}
