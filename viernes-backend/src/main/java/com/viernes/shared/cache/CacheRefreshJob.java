package com.viernes.shared.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Refresco programado de los catalogos cacheados.
 *
 * El TTL de Caffeine ya invalida las entradas por tiempo, pero este job deja
 * un refresco explicito y verificable en logs cada 10 minutos por defecto.
 * El intervalo se puede cambiar con la propiedad
 * {@code viernes.cache.refresh-ms}.
 */
@Component
public class CacheRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(CacheRefreshJob.class);

    private final CacheManager cacheManager;

    public CacheRefreshJob(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedRateString = "${viernes.cache.refresh-ms:600000}")
    public void refrescarCatalogos() {
        List<String> caches = List.of(
                CacheConfig.CACHE_PRODUCTOS,
                CacheConfig.CACHE_ETIQUETAS,
                CacheConfig.CACHE_FUENTES
        );

        caches.forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });

        log.info("Cache de catalogos refrescada: {}", caches);
    }
}
