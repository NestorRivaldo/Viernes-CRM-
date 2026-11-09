package com.viernes.mensajeria.adapters.in.web;

import com.viernes.mensajeria.application.dto.MensajeRequest;
import com.viernes.mensajeria.application.dto.MensajeResponse;
import com.viernes.mensajeria.application.dto.ResumenMensajesResponse;
import com.viernes.mensajeria.application.port.in.ManageMensajeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Mensajería", description = "Sala general de mensajes internos del CRM")
@RestController
@RequestMapping("/api/v1/mensajeria")
@RequiredArgsConstructor
public class MensajeController {

    private final ManageMensajeUseCase useCase;

    @Operation(summary = "Listar mensajes de la sala general")
    @GetMapping
    public List<MensajeResponse> listar(
            @RequestParam(required = false) Long despuesDeId,
            @RequestParam(defaultValue = "50") int limite
    ) {
        return useCase.listar(despuesDeId, limite);
    }

    @Operation(summary = "Resumen de mensajes para el contador de no leídos")
    @GetMapping("/resumen")
    public ResumenMensajesResponse resumen(@RequestParam(required = false) Long despuesDeId) {
        return useCase.resumen(despuesDeId);
    }

    @Operation(summary = "Enviar un mensaje a la sala general")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeResponse enviar(
            Authentication authentication,
            @Valid @RequestBody MensajeRequest request
    ) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException("No autenticado");
        }

        return useCase.enviar(
                authentication.getName(),
                nombreDesdePrincipal(authentication),
                request
        );
    }

    private String nombreDesdePrincipal(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        String nombre = jwt.getClaimAsString("name");
        if (nombre != null && !nombre.isBlank()) {
            return nombre.trim();
        }

        String nombres = jwt.getClaimAsString("given_name");
        String apellidos = jwt.getClaimAsString("family_name");
        String completo = String.join(" ",
                nombres == null ? "" : nombres.trim(),
                apellidos == null ? "" : apellidos.trim()
        ).trim();

        return completo.isBlank() ? null : completo;
    }
}
