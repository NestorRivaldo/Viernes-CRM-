package com.viernes.shared.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.List;

/**
 * Cache en memoria para datos de lectura frecuente y baja volatilidad.
 *
 * Los catalogos se refrescan por TTL y, ademas, el job de
 * {@link CacheRefreshJob} los invalida periodicamente para dejar evidencia
 * explicita del refresco. El dashboard usa un TTL corto porque sus KPIs
 * cambian con las operaciones del CRM.
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    public static final String CACHE_PRODUCTOS = "productos";
    public static final String CACHE_ETIQUETAS = "etiquetas";
    public static final String CACHE_FUENTES = "fuentes";
    public static final String CACHE_DASHBOARD = "dashboard";
    public static final String CACHE_DOCUMENTOS = "documentos";
    public static final String CACHE_DOCUMENTOS_NEGATIVOS = "documentos-negativos";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(CACHE_PRODUCTOS, CACHE_ETIQUETAS, CACHE_FUENTES));
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());

        // Los KPIs son mas volatiles: se mantienen solo 30 segundos.
        manager.registerCustomCache(CACHE_DASHBOARD,
                Caffeine.newBuilder()
                        .maximumSize(20)
                        .expireAfterWrite(Duration.ofSeconds(30))
                        .recordStats()
                        .build());

        // Las consultas externas DNI/RUC cambian poco y tienen cuota limitada.
        manager.registerCustomCache(CACHE_DOCUMENTOS,
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(Duration.ofHours(24))
                        .recordStats()
                        .build());

        // @Cacheable no cachea excepciones: sin esta cache, cada reintento de un
        // documento inexistente vuelve a gastar cuota. TTL corto para que un RUC
        // recien inscrito pueda consultarse de nuevo pronto.
        manager.registerCustomCache(CACHE_DOCUMENTOS_NEGATIVOS,
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .recordStats()
                        .build());

        return manager;
    }
}
