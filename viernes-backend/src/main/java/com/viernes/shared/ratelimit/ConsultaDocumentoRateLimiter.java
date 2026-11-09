package com.viernes.shared.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.viernes.shared.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limita las consultas DNI/RUC por usuario autenticado.
 *
 * El endpoint gasta cuota de un plan externo limitado y cualquier rol puede
 * crear clientes, asi que el control es por usuario y no por rol: restringirlo
 * a ADMIN/JEFE dejaria al VENDEDOR sin la consulta que usa al registrar.
 * La ventana arranca con la primera peticion de cada usuario.
 */
@Component
public class ConsultaDocumentoRateLimiter {

    private final int maxConsultas;
    private final long ventanaSegundos;
    private final Cache<String, AtomicInteger> contadores;

    public ConsultaDocumentoRateLimiter(
            @Value("${viernes.documentos.rate-limit.max-consultas:20}") int maxConsultas,
            @Value("${viernes.documentos.rate-limit.ventana-segundos:60}") long ventanaSegundos) {
        this.maxConsultas = maxConsultas;
        this.ventanaSegundos = ventanaSegundos;
        this.contadores = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(ventanaSegundos))
                .build();
    }

    public void verificar(String usuario) {
        AtomicInteger contador = contadores.get(usuario, clave -> new AtomicInteger());
        if (contador.incrementAndGet() > maxConsultas) {
            throw new RateLimitExceededException(
                    "Demasiadas consultas de documento. Espere " + ventanaSegundos
                            + " segundos antes de reintentar.",
                    ventanaSegundos);
        }
    }
}