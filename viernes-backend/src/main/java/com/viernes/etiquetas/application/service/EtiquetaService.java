package com.viernes.etiquetas.application.service;

import com.viernes.etiquetas.application.dto.EtiquetaRequest;
import com.viernes.etiquetas.application.dto.EtiquetaResponse;
import com.viernes.etiquetas.application.port.in.ManageEtiquetaUseCase;
import com.viernes.etiquetas.application.port.out.EtiquetaPersistencePort;
import com.viernes.etiquetas.domain.Etiqueta;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class EtiquetaService implements ManageEtiquetaUseCase {

    private static final String COLOR_POR_DEFECTO = "#2563EB";

    private final EtiquetaPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "etiquetas",
            key = "#activa == null ? 'all' : #activa.toString()",
            condition = "#q == null || #q.isBlank()"
    )
    public List<EtiquetaResponse> listar(Boolean activa, String q) {
        List<Etiqueta> etiquetas = activa != null
                ? persistencePort.findByActiva(activa)
                : persistencePort.findAll();

        String filtro = normalizar(q);

        return etiquetas.stream()
                .filter(e -> filtro.isBlank() || e.getNombreNormalizado().contains(filtro))
                .sorted(Comparator.comparing(Etiqueta::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "etiquetas", key = "#id")
    public EtiquetaResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @CacheEvict(cacheNames = "etiquetas", allEntries = true)
    public EtiquetaResponse crear(EtiquetaRequest request) {
        String nombre = limpiarNombre(request.nombre());
        String nombreNormalizado = normalizar(nombre);

        if (persistencePort.existsByNombreNormalizado(nombreNormalizado)) {
            throw new IllegalArgumentException("Ya existe una etiqueta con el nombre: " + nombre);
        }

        Etiqueta etiqueta = Etiqueta.builder()
                .nombre(nombre)
                .nombreNormalizado(nombreNormalizado)
                .color(normalizarColor(request.color()))
                .descripcion(limpiarDescripcion(request.descripcion()))
                .activa(request.activa() != null ? request.activa() : true)
                .creadoEn(LocalDateTime.now())
                .build();

        return toResponse(persistencePort.save(etiqueta));
    }

    @Override
    @CacheEvict(cacheNames = "etiquetas", allEntries = true)
    public EtiquetaResponse actualizar(Long id, EtiquetaRequest request) {
        Etiqueta etiqueta = findOrThrow(id);
        String nombre = limpiarNombre(request.nombre());
        String nombreNormalizado = normalizar(nombre);

        if (persistencePort.existsByNombreNormalizadoAndIdNot(nombreNormalizado, id)) {
            throw new IllegalArgumentException("Ya existe una etiqueta con el nombre: " + nombre);
        }

        etiqueta.setNombre(nombre);
        etiqueta.setNombreNormalizado(nombreNormalizado);
        etiqueta.setColor(normalizarColor(request.color()));
        etiqueta.setDescripcion(limpiarDescripcion(request.descripcion()));
        if (request.activa() != null) {
            etiqueta.setActiva(request.activa());
        }

        return toResponse(persistencePort.save(etiqueta));
    }

    /**
     * Archiva la etiqueta. No se elimina físicamente para conservar el
     * histórico de actividades que la utilizaron.
     */
    @Override
    @CacheEvict(cacheNames = "etiquetas", allEntries = true)
    public void eliminar(Long id) {
        Etiqueta etiqueta = findOrThrow(id);
        etiqueta.setActiva(false);
        persistencePort.save(etiqueta);
    }

    private Etiqueta findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etiqueta no encontrada con ID: " + id));
    }

    private String limpiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la etiqueta es obligatorio");
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > 40) {
            throw new IllegalArgumentException("El nombre de la etiqueta no puede superar los 40 caracteres");
        }
        return limpio;
    }

    private String limpiarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        String limpia = descripcion.trim();
        return limpia.length() > 160 ? limpia.substring(0, 160) : limpia;
    }

    private String normalizarColor(String color) {
        if (color == null || color.isBlank()) {
            return COLOR_POR_DEFECTO;
        }
        return color.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private EtiquetaResponse toResponse(Etiqueta etiqueta) {
        return new EtiquetaResponse(
                etiqueta.getId(),
                etiqueta.getNombre(),
                etiqueta.getColor(),
                etiqueta.getDescripcion(),
                etiqueta.getActiva(),
                etiqueta.getCreadoEn()
        );
    }
}
