package com.viernes.oportunidades.application.service;

import com.viernes.oportunidades.application.dto.OportunidadRequest;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.application.port.in.ManageOportunidadUseCase;
import com.viernes.oportunidades.application.port.out.OportunidadPersistencePort;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import com.viernes.oportunidades.domain.Oportunidad;
import com.viernes.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OportunidadService implements ManageOportunidadUseCase {

    private final OportunidadPersistencePort persistencePort;

    @Override
    public List<OportunidadResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<OportunidadResponse> buscar(String q) {
        return persistencePort.findByNombreOrClienteNombre(q).stream().map(this::toResponse).toList();
    }

    @Override
    public List<OportunidadResponse> filtrarPorEtapa(EtapaOportunidad etapa) {
        return persistencePort.findByEtapa(etapa).stream().map(this::toResponse).toList();
    }

    @Override
    public OportunidadResponse obtener(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public OportunidadResponse crear(OportunidadRequest request) {
        Oportunidad o = Oportunidad.builder()
            .creadoEn(LocalDateTime.now())
            .etapa(request.etapa() != null ? request.etapa() : EtapaOportunidad.PROSPECCION)
            .build();
        apply(o, request);
        return toResponse(persistencePort.save(o));
    }

    @Override
    @Transactional
    public OportunidadResponse actualizar(Long id, OportunidadRequest request) {
        Oportunidad o = findOrThrow(id);
        apply(o, request);
        return toResponse(persistencePort.save(o));
    }

    @Override
    @Transactional
    public OportunidadResponse cambiarEtapa(Long id, EtapaOportunidad etapa) {
        Oportunidad o = findOrThrow(id);
        o.setEtapa(etapa);
        return toResponse(persistencePort.save(o));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        findOrThrow(id);
        persistencePort.deleteById(id);
    }

    private Oportunidad findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidad no encontrada: " + id));
    }

    private void apply(Oportunidad o, OportunidadRequest r) {
        o.setNombre(r.nombre());
        o.setClienteNombre(r.clienteNombre());
        o.setValor(r.valor());
        o.setProbabilidad(r.probabilidad());
        o.setResponsable(r.responsable());
        o.setFechaCierre(r.fechaCierre());
        if (r.etapa() != null) o.setEtapa(r.etapa());
    }

    private OportunidadResponse toResponse(Oportunidad o) {
        return new OportunidadResponse(o.getId(), o.getNombre(), o.getClienteNombre(),
                o.getValor(), o.getProbabilidad(), o.getValorPonderado(),
                o.getEtapa(), o.getResponsable(), o.getFechaCierre(), o.getCreadoEn());
    }
}
