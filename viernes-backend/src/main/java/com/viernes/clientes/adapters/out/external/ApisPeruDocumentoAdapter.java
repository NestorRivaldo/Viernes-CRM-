package com.viernes.clientes.adapters.out.external;

import com.viernes.clientes.adapters.out.external.dto.ApisPeruDniResponse;
import com.viernes.clientes.adapters.out.external.dto.ApisPeruRucResponse;
import com.viernes.clientes.application.port.out.DocumentoLookupPort;
import com.viernes.clientes.domain.DocumentoConsultado;
import com.viernes.clientes.domain.TipoDocumento;
import com.viernes.shared.cache.CacheConfig;
import com.viernes.shared.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ApisPeruDocumentoAdapter implements DocumentoLookupPort {

    private final ApisPeruProperties properties;
    private final CacheManager cacheManager;
    private RestClient restClient;
    private Cache cacheNegativos;

    @PostConstruct
    void inicializarCliente() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (int) properties.getTimeout().toMillis();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();

        this.cacheNegativos = cacheManager.getCache(CacheConfig.CACHE_DOCUMENTOS_NEGATIVOS);
    }

    @Override
    @Cacheable(
            cacheNames = CacheConfig.CACHE_DOCUMENTOS,
            key = "#tipoDocumento.name() + ':' + #numeroDocumento"
    )
    public DocumentoConsultado consultar(TipoDocumento tipoDocumento, String numeroDocumento) {
        validarConfiguracion();

        String clave = tipoDocumento.name() + ':' + numeroDocumento;
        String mensajeNegativo = cacheNegativos.get(clave, String.class);
        if (mensajeNegativo != null) {
            throw new ResourceNotFoundException(mensajeNegativo);
        }

        try {
            return switch (tipoDocumento) {
                case DNI -> consultarDni(numeroDocumento);
                case RUC -> consultarRuc(numeroDocumento);
            };
        } catch (ResourceNotFoundException ex) {
            cacheNegativos.put(clave, ex.getMessage());
            throw ex;
        } catch (RestClientResponseException ex) {
            // Solo el 404 es un negativo real. 401/403/429/5xx son transitorios o de
            // configuracion: cachearlos impediria recuperarse sin esperar el TTL.
            if (ex.getStatusCode().value() == 404) {
                cacheNegativos.put(clave, mensajeError(ex));
            }
            throw new IllegalStateException(mensajeError(ex), ex);
        } catch (ResourceAccessException ex) {
            throw new IllegalStateException(
                    "No se pudo conectar con el servicio de consulta DNI/RUC. Intente nuevamente.", ex);
        }
    }

    private DocumentoConsultado consultarDni(String numeroDocumento) {
        ApisPeruDniResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/dni/{numero}")
                        .queryParam("token", properties.getToken())
                        .build(numeroDocumento))
                .retrieve()
                .body(ApisPeruDniResponse.class);

        validarRespuesta(response != null ? response.success() : null,
                response != null ? response.message() : null,
                "No se encontró el DNI en el servicio de consulta");

        if (response == null || (response.dni() == null && response.nombres() == null)) {
            throw new ResourceNotFoundException("No se encontró el DNI en el servicio de consulta");
        }

        String nombreCompleto = Stream.of(
                        response.nombres(),
                        response.apellidoPaterno(),
                        response.apellidoMaterno())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .collect(Collectors.joining(" "));

        return new DocumentoConsultado(
                TipoDocumento.DNI,
                response.dni() != null ? response.dni() : numeroDocumento,
                nombreCompleto,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    private DocumentoConsultado consultarRuc(String numeroDocumento) {
        ApisPeruRucResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ruc/{numero}")
                        .queryParam("token", properties.getToken())
                        .build(numeroDocumento))
                .retrieve()
                .body(ApisPeruRucResponse.class);

        validarRespuesta(response != null ? response.success() : null,
                response != null ? response.message() : null,
                "No se encontró el RUC en el servicio de consulta");

        if (response == null || (response.ruc() == null && response.razonSocial() == null)) {
            throw new ResourceNotFoundException("No se encontró el RUC en el servicio de consulta");
        }

        List<String> telefonos = response.telefonos() == null
                ? List.of()
                : response.telefonos().stream().filter(Objects::nonNull).toList();

        return new DocumentoConsultado(
                TipoDocumento.RUC,
                response.ruc() != null ? response.ruc() : numeroDocumento,
                null,
                response.razonSocial(),
                response.nombreComercial(),
                response.estado(),
                response.condicion(),
                response.direccion(),
                telefonos
        );
    }

    private void validarConfiguracion() {
        if (properties.getToken() == null || properties.getToken().isBlank()) {
            throw new IllegalStateException(
                    "La consulta externa DNI/RUC no está configurada. Defina la variable APISPERU_TOKEN.");
        }
    }

    private void validarRespuesta(Boolean success, String message, String mensajeDefault) {
        if (Boolean.FALSE.equals(success)) {
            throw new ResourceNotFoundException(
                    message != null && !message.isBlank() ? message : mensajeDefault);
        }
    }

    private String mensajeError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 401 || status == 403) {
            return "No se pudo autenticar con APIsPERU. Verifique el token configurado.";
        }
        if (status == 404) {
            return "No se encontró el documento en APIsPERU.";
        }
        if (status == 429) {
            return "Se agotó la cuota de consultas DNI/RUC. Intente más tarde o revise el plan contratado.";
        }
        return "APIsPERU no pudo procesar la consulta (HTTP " + status + ").";
    }
}
