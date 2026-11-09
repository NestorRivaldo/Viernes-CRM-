package com.viernes.shared.ratelimit;

import com.viernes.shared.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultaDocumentoRateLimiterTest {

    @Test
    void permiteHastaElLimiteConfigurado() {
        ConsultaDocumentoRateLimiter limiter = new ConsultaDocumentoRateLimiter(3, 60);

        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> limiter.verificar("vendedor"));
        }
    }

    @Test
    void superadoElLimite_lanzaExcepcionConLaVentana() {
        ConsultaDocumentoRateLimiter limiter = new ConsultaDocumentoRateLimiter(2, 45);
        limiter.verificar("vendedor");
        limiter.verificar("vendedor");

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> limiter.verificar("vendedor"));

        assertEquals(45, ex.getRetryAfterSegundos());
    }

    @Test
    void elLimiteEsIndependientePorUsuario() {
        ConsultaDocumentoRateLimiter limiter = new ConsultaDocumentoRateLimiter(1, 60);
        limiter.verificar("vendedor");

        assertThrows(RateLimitExceededException.class, () -> limiter.verificar("vendedor"));
        assertDoesNotThrow(() -> limiter.verificar("jefe"));
    }

    @Test
    void peticionesConcurrentes_respetanElLimiteExacto() throws Exception {
        int limite = 5;
        int hilos = 20;
        ConsultaDocumentoRateLimiter limiter = new ConsultaDocumentoRateLimiter(limite, 60);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger aceptadas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();

        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                tareas.add(pool.submit(() -> {
                    largada.await();
                    try {
                        limiter.verificar("vendedor");
                        aceptadas.incrementAndGet();
                    } catch (RateLimitExceededException ex) {
                        rechazadas.incrementAndGet();
                    }
                    return null;
                }));
            }
            largada.countDown();
            for (Future<?> tarea : tareas) {
                tarea.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(limite, aceptadas.get());
        assertEquals(hilos - limite, rechazadas.get());
    }
}