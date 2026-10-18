package com.viernes.actividades.application.port.in;

import com.viernes.actividades.application.dto.ActividadRequest;
import com.viernes.actividades.application.dto.ActividadResponse;
import com.viernes.actividades.domain.EstadoActividad;
import java.util.List;

public interface ManageActividadUseCase {
    ActividadResponse crear(ActividadRequest request);
    ActividadResponse actualizar(Long id, ActividadRequest request);
    ActividadResponse obtener(Long id);
    List<ActividadResponse> listarTodos();
    List<ActividadResponse> listarVencidas();
    List<ActividadResponse> filtrarPorEstado(EstadoActividad estado);
    List<ActividadResponse> filtrarPorCliente(Long idCliente);
    List<ActividadResponse> filtrarPorLead(Long idLead);
    List<ActividadResponse> filtrarPorOportunidad(Long idOportunidad);
    List<ActividadResponse> filtrarPorUsuario(String idUsuario);
    ActividadResponse cambiarEstado(Long id, EstadoActividad estado);
    void eliminar(Long id);
}
