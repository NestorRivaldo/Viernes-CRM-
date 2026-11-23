package com.viernes.clinica.application.service;

import com.viernes.clinica.adapters.out.persistence.entity.ClinicaEntity;
import com.viernes.clinica.adapters.out.persistence.entity.ConsultorioEntity;
import com.viernes.clinica.adapters.out.persistence.entity.EspecialidadEntity;
import com.viernes.clinica.adapters.out.persistence.repository.ClinicaRepository;
import com.viernes.clinica.adapters.out.persistence.repository.ConsultorioRepository;
import com.viernes.clinica.adapters.out.persistence.repository.EspecialidadRepository;
import com.viernes.clinica.application.dto.ConsultorioRequest;
import com.viernes.clinica.application.dto.ConsultorioResponse;
import com.viernes.clinica.application.dto.EspecialidadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultorioService {

    private final ConsultorioRepository consultorioRepository;
    private final ClinicaRepository clinicaRepository;
    private final EspecialidadRepository especialidadRepository;

    @Transactional(readOnly = true)
    public List<EspecialidadResponse> listarEspecialidades() {
        return especialidadRepository.findAll().stream()
                .map(e -> new EspecialidadResponse(e.getId(), e.getCodigo(), e.getNombre()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultorioResponse> listarConsultorios() {
        return consultorioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultorioResponse obtenerConsultorio(Long id) {
        ConsultorioEntity entity = consultorioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultorio no encontrado con ID: " + id));
        return toResponse(entity);
    }

    public ConsultorioResponse crearConsultorio(ConsultorioRequest request) {
        ClinicaEntity clinica = clinicaRepository.findById(request.idClinica())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clínica no encontrada con ID: " + request.idClinica()));

        if (consultorioRepository.existsByCodigo(request.codigo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un consultorio con el código: " + request.codigo());
        }

        Set<EspecialidadEntity> especialidades = new HashSet<>();
        if (request.especialidadIds() != null && !request.especialidadIds().isEmpty()) {
            especialidades.addAll(especialidadRepository.findAllById(request.especialidadIds()));
        }

        ConsultorioEntity entity = ConsultorioEntity.builder()
                .clinica(clinica)
                .codigo(request.codigo())
                .tarifaHora(request.tarifaHora())
                .estado(request.estado())
                .especialidades(especialidades)
                .build();

        return toResponse(consultorioRepository.save(entity));
    }

    public void eliminarConsultorio(Long id) {
        if (!consultorioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consultorio no encontrado con ID: " + id);
        }
        consultorioRepository.deleteById(id);
    }

    private ConsultorioResponse toResponse(ConsultorioEntity entity) {
        List<EspecialidadResponse> especList = entity.getEspecialidades() == null ? List.of() :
                entity.getEspecialidades().stream()
                        .map(e -> new EspecialidadResponse(e.getId(), e.getCodigo(), e.getNombre()))
                        .toList();

        return new ConsultorioResponse(
                entity.getId(),
                entity.getClinica() != null ? entity.getClinica().getId() : null,
                entity.getClinica() != null ? entity.getClinica().getRazonSocial() : "",
                entity.getCodigo(),
                entity.getTarifaHora(),
                entity.getEstado(),
                especList
        );
    }
}
