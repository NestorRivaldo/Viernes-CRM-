package com.viernes.leads.application.dto;

import com.viernes.leads.domain.EstadoLead;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LeadRequest(
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 150) String empresa,
    @Email @Size(max = 150) String correo,
    @Size(max = 30) String telefono,
    @Size(max = 80) String fuente,
    EstadoLead estado,
    @Size(max = 100) String responsable,
    LocalDate fecha
) {}
