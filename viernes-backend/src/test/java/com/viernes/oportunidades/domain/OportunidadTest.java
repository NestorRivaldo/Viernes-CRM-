package com.viernes.oportunidades.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OportunidadTest {

    @Test
    void getValorPonderado_valorNull_devuelveZero() {
        Oportunidad oportunidad = Oportunidad.builder()
                .valor(null)
                .probabilidad(50)
                .build();

        assertEquals(BigDecimal.ZERO, oportunidad.getValorPonderado());
    }

    @Test
    void getValorPonderado_probabilidadNull_devuelveZero() {
        Oportunidad oportunidad = Oportunidad.builder()
                .valor(new BigDecimal("10000.00"))
                .probabilidad(null)
                .build();

        assertEquals(BigDecimal.ZERO, oportunidad.getValorPonderado());
    }

    @Test
    void getValorPonderado_ambosNull_devuelveZero() {
        Oportunidad oportunidad = Oportunidad.builder().build();

        assertEquals(BigDecimal.ZERO, oportunidad.getValorPonderado());
    }

    @Test
    void getValorPonderado_decimalesNoExactos_redondeaHalfUp() {
        Oportunidad oportunidad = Oportunidad.builder()
                .valor(new BigDecimal("10.01"))
                .probabilidad(33)
                .build();

        assertEquals(new BigDecimal("3.30"), oportunidad.getValorPonderado());
    }

    @Test
    void getValorPonderado_valoresExactos_devuelveEscalaDos() {
        Oportunidad oportunidad = Oportunidad.builder()
                .valor(new BigDecimal("100"))
                .probabilidad(50)
                .build();

        assertEquals(new BigDecimal("50.00"), oportunidad.getValorPonderado());
    }
}
