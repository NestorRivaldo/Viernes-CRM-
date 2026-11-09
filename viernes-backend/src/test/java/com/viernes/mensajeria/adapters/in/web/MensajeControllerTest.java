package com.viernes.mensajeria.adapters.in.web;

import com.viernes.mensajeria.application.dto.MensajeRequest;
import com.viernes.mensajeria.application.dto.MensajeResponse;
import com.viernes.mensajeria.application.dto.ResumenMensajesResponse;
import com.viernes.mensajeria.application.port.in.ManageMensajeUseCase;
import com.viernes.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MensajeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManageMensajeUseCase useCase;

    @InjectMocks
    private MensajeController controller;

    private MensajeResponse mensaje;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mensaje = new MensajeResponse(
                1L, "admin@viernes.com", "Admin Viernes", "Hola equipo", LocalDateTime.now()
        );
    }

    @Test
    void listar_retornaMensajes() throws Exception {
        when(useCase.listar(null, 50)).thenReturn(List.of(mensaje));

        mockMvc.perform(get("/api/v1/mensajeria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].contenido").value("Hola equipo"));
    }

    @Test
    void resumen_retornaTotalYUltimoId() throws Exception {
        when(useCase.resumen(null)).thenReturn(new ResumenMensajesResponse(1L, 1L));

        mockMvc.perform(get("/api/v1/mensajeria/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1L))
                .andExpect(jsonPath("$.ultimoId").value(1L));
    }

    @Test
    void enviar_conAuthentication_retornaCreated() throws Exception {
        when(useCase.enviar(eq("admin@viernes.com"), isNull(), any(MensajeRequest.class)))
                .thenReturn(mensaje);

        mockMvc.perform(post("/api/v1/mensajeria")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"Hola equipo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contenido").value("Hola equipo"));
    }

    @Test
    void enviar_conJwt_usaNombreDelToken() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("admin@viernes.com")
                .claim("name", "Nestor Barrantes")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        MensajeResponse respuesta = new MensajeResponse(
                2L, "admin@viernes.com", "Nestor Barrantes", "Hola equipo", LocalDateTime.now()
        );
        when(useCase.enviar(eq("admin@viernes.com"), eq("Nestor Barrantes"), any(MensajeRequest.class)))
                .thenReturn(respuesta);

        mockMvc.perform(post("/api/v1/mensajeria")
                        .principal(new JwtAuthenticationToken(jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"Hola equipo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioNombre").value("Nestor Barrantes"));
    }

    @Test
    void enviar_contenidoEnBlanco_retornaBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/mensajeria")
                        .principal(autenticacion())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contenido\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    private UsernamePasswordAuthenticationToken autenticacion() {
        return new UsernamePasswordAuthenticationToken("admin@viernes.com", null, List.of());
    }
}
