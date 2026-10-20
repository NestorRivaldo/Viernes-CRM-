package com.viernes.equipos.application.port.in;

import com.viernes.equipos.application.dto.EquipoRequest;
import com.viernes.equipos.application.dto.EquipoResponse;
import com.viernes.equipos.application.dto.MiembroResponse;
import com.viernes.equipos.domain.EstadoEquipo;
import java.util.List;

public interface ManageEquipoUseCase {
    List<EquipoResponse> listar();
    List<EquipoResponse> listarPorEstado(EstadoEquipo estado);
    EquipoResponse obtener(Long id);
    EquipoResponse crear(EquipoRequest request);
    EquipoResponse actualizar(Long id, EquipoRequest request);
    void activar(Long id);
    void desactivar(Long id);
    void eliminar(Long id);

    MiembroResponse agregarMiembro(Long equipoId, Long usuarioId);
    void quitarMiembro(Long equipoId, Long usuarioId);
    List<MiembroResponse> listarMiembros(Long equipoId);
}
