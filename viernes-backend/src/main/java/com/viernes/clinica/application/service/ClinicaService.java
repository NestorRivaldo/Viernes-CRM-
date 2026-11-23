package com.viernes.clinica.application.service;

import com.viernes.clinica.adapters.out.persistence.entity.ClinicaEntity;
import com.viernes.clinica.adapters.out.persistence.repository.ClinicaRepository;
import com.viernes.clinica.application.dto.ClinicaRequest;
import com.viernes.clinica.application.dto.ClinicaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicaService {

    private final ClinicaRepository repository;

    @Transactional(readOnly = true)
    public List<ClinicaResponse> listar() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClinicaResponse obtener(Long id) {
        ClinicaEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica no encontrada con ID: " + id));
        return toResponse(entity);
    }

    public ClinicaResponse crear(ClinicaRequest request) {
        if (repository.existsByRuc(request.ruc())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una clínica con el RUC: " + request.ruc());
        }

        ClinicaEntity entity = ClinicaEntity.builder()
                .ruc(request.ruc())
                .razonSocial(request.razonSocial())
                .telefono(request.telefono())
                .activo(request.activo() == null || request.activo() ? 1 : 0)
                .build();

        return toResponse(repository.save(entity));
    }

    public ClinicaResponse actualizar(Long id, ClinicaRequest request) {
        ClinicaEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica no encontrada con ID: " + id));

        if (repository.existsByRucAndIdNot(request.ruc(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe otra clínica con el RUC: " + request.ruc());
        }

        entity.setRuc(request.ruc());
        entity.setRazonSocial(request.razonSocial());
        entity.setTelefono(request.telefono());
        entity.setActivo(request.activo() == null || request.activo() ? 1 : 0);

        return toResponse(repository.save(entity));
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica no encontrada con ID: " + id);
        }
        repository.deleteById(id);
    }

    public ClinicaResponse toResponse(ClinicaEntity entity) {
        return new ClinicaResponse(
                entity.getId(),
                entity.getRuc(),
                entity.getRazonSocial(),
                entity.getTelefono(),
                entity.getActivo() != null && entity.getActivo() == 1
        );
    }
}
