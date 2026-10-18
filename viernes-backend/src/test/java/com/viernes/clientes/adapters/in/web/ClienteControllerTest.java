package com.viernes.clientes.adapters.in.web;

import com.viernes.clientes.application.dto.ClienteRequest;
import com.viernes.clientes.application.dto.ClienteResponse;
import com.viernes.clientes.application.dto.ContactoRequest;
import com.viernes.clientes.application.dto.ContactoResponse;
import com.viernes.clientes.application.port.in.ManageClienteUseCase;
import com.viernes.clientes.domain.EstadoCliente;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageClienteUseCase useCase;

    @InjectMocks
    private ClienteController controller;

    private ClienteResponse validResponse;
    private ContactoResponse validContactoResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.viernes.shared.exception.GlobalExceptionHandler())
                .build();

        validResponse = new ClienteResponse(
                1L, "Juan Perez", "Empresa SA", "juan@mail.com", "555-1234",
                EstadoCliente.ACTIVO, "Maria", LocalDateTime.now(), List.of()
        );

        validContactoResponse = new ContactoResponse(
                10L, 1L, "Ana", "Lopez", "Gerente", "ana@mail.com", "555-1111", false, LocalDateTime.now()
        );
    }

    @Test
    void listar_clientes_retornaOk() throws Exception {
        when(useCase.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void obtener_clienteExistente_retornaOk() throws Exception {
        when(useCase.obtener(1L)).thenReturn(validResponse);

        mockMvc.perform(get("/api/v1/clientes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nombre").value("Juan Perez"));
    }

    @Test
    void obtener_clienteInexistente_retornaNotFound() throws Exception {
        when(useCase.obtener(99L)).thenThrow(new ResourceNotFoundException("No encontrado"));

        mockMvc.perform(get("/api/v1/clientes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_clienteValido_retornaCreated() throws Exception {
        when(useCase.crear(any(ClienteRequest.class))).thenReturn(validResponse);

        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Juan Perez\",\"empresa\":\"Empresa SA\",\"correo\":\"juan@mail.com\",\"telefono\":\"555-1234\",\"responsable\":\"Maria\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void crear_clienteInvalido_nombreEnBlanco_retornaBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"empresa\":\"Empresa SA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_clienteExistente_retornaOk() throws Exception {
        when(useCase.actualizar(eq(1L), any(ClienteRequest.class))).thenReturn(validResponse);

        mockMvc.perform(put("/api/v1/clientes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Juan Perez\",\"empresa\":\"Empresa SA\",\"correo\":\"juan@mail.com\",\"telefono\":\"555-1234\",\"responsable\":\"Maria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void desactivar_clienteExistente_retornaNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/clientes/1/desactivar"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_clienteExistente_retornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/clientes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listarContactos_clienteExistente_retornaOk() throws Exception {
        when(useCase.listarContactos(1L)).thenReturn(List.of(validContactoResponse));

        mockMvc.perform(get("/api/v1/clientes/1/contactos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
    }

    @Test
    void agregarContacto_clienteExistente_retornaCreated() throws Exception {
        when(useCase.agregarContacto(eq(1L), any(ContactoRequest.class))).thenReturn(validContactoResponse);

        mockMvc.perform(post("/api/v1/clientes/1/contactos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"cargo\":\"Gerente\",\"correo\":\"ana@mail.com\",\"telefono\":\"555-1111\",\"principal\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void actualizarContacto_contactoExistente_retornaOk() throws Exception {
        when(useCase.actualizarContacto(eq(1L), eq(10L), any(ContactoRequest.class))).thenReturn(validContactoResponse);

        mockMvc.perform(put("/api/v1/clientes/1/contactos/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Maria\",\"apellido\":\"Lopez\",\"cargo\":\"Directora\",\"correo\":\"anamaria@mail.com\",\"telefono\":\"555-2222\",\"principal\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    void eliminarContacto_contactoExistente_retornaNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/clientes/1/contactos/10"))
                .andExpect(status().isNoContent());
    }
}
