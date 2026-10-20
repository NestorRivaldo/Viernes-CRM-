package com.viernes.marketing.application.dto;

import jakarta.validation.constraints.NotNull;

public record CampaniaLeadRequest(
    @NotNull Long leadId
) {}
