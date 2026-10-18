package com.viernes.usuarios.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viernes.usuarios.application.dto.UsuarioRequest;
import com.viernes.usuarios.application.dto.UsuarioResponse;
import com.viernes.usuarios.application.port.in.ManageUsuarioUseCase;
import com.viernes.usuarios.domain.EstadoUsuario;
import com.viernes.usuarios.domain.RolUsuario;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ManageUsuarioUseCase useCase;

    @InjectMocks
    private UsuarioController controller;

    private UsuarioRequest validRequest;
    private UsuarioResponse validResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validRequest = new UsuarioRequest(
                "Juan", "Pérez", "juan.perez@example.com", "secreto123", RolUsuario.VENDEDOR
        );
        validResponse = new UsuarioResponse(
                1L, "Juan", "Pérez", "juan.perez@example.com",
                RolUsuario.VENDEDOR, EstadoUsuario.ACTIVO, LocalDateTime.now()
        );
    }

    @Test
    void listarUsuarios() throws Exception {
        when(useCase.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void obtenerUsuarioExistente() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.correo").value("juan.perez@example.com"));
    }

    @Test
    void obtenerUsuarioInexistente() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/usuarios/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearUsuarioValido() throws Exception {
        when(useCase.crear(any(UsuarioRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.correo").value("juan.perez@example.com"))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crearUsuarioInvalido() throws Exception {
        UsuarioRequest invalidRequest = new UsuarioRequest(
                "", "Pérez", "correo-invalido", "secreto123", RolUsuario.VENDEDOR // Inválido: nombre en blanco y correo malformado
        );

        mockMvc.perform(post("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarUsuario() throws Exception {
        when(useCase.actualizar(eq(1L), any(UsuarioRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/usuarios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void actualizarUsuarioConCorreoDuplicado() throws Exception {
        when(useCase.actualizar(eq(1L), any(UsuarioRequest.class)))
                .thenThrow(new IllegalArgumentException("Ya existe un usuario con el correo: juan.perez@example.com"));

        mockMvc.perform(put("/api/v1/usuarios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activarUsuario() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/1/activar"))
                .andExpect(status().isNoContent());
    }

    @Test
    void desactivarUsuario() throws Exception {
        mockMvc.perform(patch("/api/v1/usuarios/1/desactivar"))
                .andExpect(status().isNoContent());
    }
}
