package com.viernes.clientes.adapters.out.external;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.sun.net.httpserver.HttpServer;
import com.viernes.clientes.domain.DocumentoConsultado;
import com.viernes.clientes.domain.TipoDocumento;
import com.viernes.shared.cache.CacheConfig;
import com.viernes.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Usa un servidor HTTP del propio JDK para contar las peticiones reales que
 * salen hacia el proveedor: es la unica forma de probar que un negativo
 * cacheado no vuelve a gastar cuota, sin depender de APIsPERU ni de su token.
 */
class ApisPeruDocumentoAdapterTest {

    private static final String DNI_VALIDO =
            "{\"dni\":\"12345678\",\"nombres\":\"JUAN\",\"apellidoPaterno\":\"PEREZ\",\"apellidoMaterno\":\"LOPEZ\"}";

    private HttpServer servidor;
    private ApisPeruDocumentoAdapter adapter;

    private final AtomicInteger peticiones = new AtomicInteger();
    private final AtomicInteger estadoHttp = new AtomicInteger(200);
    private final AtomicReference<String> cuerpo = new AtomicReference<>("{}");

    @BeforeEach
    void setUp() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            peticiones.incrementAndGet();
            byte[] bytes = cuerpo.get().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estadoHttp.get(), bytes.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();

        adapter = crearAdapter("token-de-prueba");
    }

    @AfterEach
    void tearDown() {
        servidor.stop(0);
    }

    @Test
    void dniEncontrado_devuelveNombreCompleto() {
        cuerpo.set(DNI_VALIDO);

        DocumentoConsultado resultado = adapter.consultar(TipoDocumento.DNI, "12345678");

        assertEquals("JUAN PEREZ LOPEZ", resultado.nombreCompleto());
        assertEquals("12345678", resultado.numeroDocumento());
        assertEquals(1, peticiones.get());
    }

    @Test
    void rucEncontrado_devuelveRazonSocialYTelefonos() {
        cuerpo.set("{\"ruc\":\"20123456789\",\"razonSocial\":\"EMPRESA SA\",\"nombreComercial\":\"VIERNES\","
                + "\"estado\":\"ACTIVO\",\"condicion\":\"HABIDO\",\"direccion\":\"AV. SIEMPRE VIVA 123\","
                + "\"telefonos\":[\"999888777\",null]}");

        DocumentoConsultado resultado = adapter.consultar(TipoDocumento.RUC, "20123456789");

        assertEquals("EMPRESA SA", resultado.razonSocial());
        assertEquals("AV. SIEMPRE VIVA 123", resultado.direccion());
        assertEquals(List.of("999888777"), resultado.telefonos());
        assertEquals(1, peticiones.get());
    }

    @Test
    void dniNoEncontrado_seCacheaYNoVuelveAGastarCuota() {
        cuerpo.set("{\"success\":false,\"message\":\"DNI no encontrado\"}");

        ResourceNotFoundException primera = assertThrows(ResourceNotFoundException.class,
                () -> adapter.consultar(TipoDocumento.DNI, "99999999"));
        assertEquals("DNI no encontrado", primera.getMessage());
        assertEquals(1, peticiones.get());

        ResourceNotFoundException segunda = assertThrows(ResourceNotFoundException.class,
                () -> adapter.consultar(TipoDocumento.DNI, "99999999"));
        assertEquals("DNI no encontrado", segunda.getMessage());
        assertEquals(1, peticiones.get());
    }

    @Test
    void http404_seCacheaComoNegativo() {
        estadoHttp.set(404);
        cuerpo.set("{\"message\":\"not found\"}");

        assertThrows(IllegalStateException.class,
                () -> adapter.consultar(TipoDocumento.RUC, "20123456789"));
        assertEquals(1, peticiones.get());

        assertThrows(ResourceNotFoundException.class,
                () -> adapter.consultar(TipoDocumento.RUC, "20123456789"));
        assertEquals(1, peticiones.get());
    }

    @Test
    void agotadaLaCuota_noSeCacheaYPermiteRecuperarse() {
        estadoHttp.set(429);
        cuerpo.set("{\"message\":\"quota exceeded\"}");

        assertThrows(IllegalStateException.class,
                () -> adapter.consultar(TipoDocumento.DNI, "12345678"));
        assertEquals(1, peticiones.get());

        estadoHttp.set(200);
        cuerpo.set(DNI_VALIDO);

        DocumentoConsultado resultado = adapter.consultar(TipoDocumento.DNI, "12345678");

        assertEquals("JUAN PEREZ LOPEZ", resultado.nombreCompleto());
        assertEquals(2, peticiones.get());
    }

    @Test
    void errorDelProveedor_noSeCachea() {
        estadoHttp.set(500);
        cuerpo.set("{\"message\":\"internal error\"}");

        assertThrows(IllegalStateException.class,
                () -> adapter.consultar(TipoDocumento.DNI, "12345678"));
        assertThrows(IllegalStateException.class,
                () -> adapter.consultar(TipoDocumento.DNI, "12345678"));

        assertEquals(2, peticiones.get());
    }

    @Test
    void elNegativoEsPorDocumento_noAfectaAOtro() {
        cuerpo.set("{\"success\":false,\"message\":\"DNI no encontrado\"}");
        assertThrows(ResourceNotFoundException.class,
                () -> adapter.consultar(TipoDocumento.DNI, "99999999"));

        cuerpo.set(DNI_VALIDO);
        DocumentoConsultado resultado = adapter.consultar(TipoDocumento.DNI, "12345678");

        assertEquals("JUAN PEREZ LOPEZ", resultado.nombreCompleto());
        assertEquals(2, peticiones.get());
    }

    @Test
    void sinToken_fallaSinConsumirCuota() {
        ApisPeruDocumentoAdapter sinToken = crearAdapter("");

        assertThrows(IllegalStateException.class,
                () -> sinToken.consultar(TipoDocumento.DNI, "12345678"));

        assertEquals(0, peticiones.get());
    }

    private ApisPeruDocumentoAdapter crearAdapter(String token) {
        ApisPeruProperties properties = new ApisPeruProperties();
        properties.setBaseUrl("http://127.0.0.1:" + servidor.getAddress().getPort());
        properties.setToken(token);
        properties.setTimeout(Duration.ofSeconds(5));

        ApisPeruDocumentoAdapter nuevo = new ApisPeruDocumentoAdapter(properties, cacheManager());
        nuevo.inicializarCliente();
        return nuevo;
    }

    private CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(CacheConfig.CACHE_DOCUMENTOS_NEGATIVOS,
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(Duration.ofMinutes(10))
                        .build());
        return manager;
    }
}