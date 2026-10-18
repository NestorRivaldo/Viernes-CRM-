package com.viernes.propuestas.application.port.in;

import com.viernes.propuestas.application.dto.PropuestaRequest;
import com.viernes.propuestas.application.dto.PropuestaResponse;
import com.viernes.propuestas.domain.EstadoPropuesta;
import java.util.List;

public interface ManagePropuestaUseCase {
    PropuestaResponse crear(PropuestaRequest request);
    PropuestaResponse obtener(Long id);
    List<PropuestaResponse> listar();
    List<PropuestaResponse> listarPorOportunidad(Long idOportunidad);
    List<PropuestaResponse> listarPorCliente(Long idCliente);
    List<PropuestaResponse> filtrarPorEstado(EstadoPropuesta estado);
    PropuestaResponse actualizar(Long id, PropuestaRequest request);
    PropuestaResponse enviar(Long id);
    PropuestaResponse aprobar(Long id);
    PropuestaResponse rechazar(Long id);
    PropuestaResponse cancelar(Long id);
    void eliminar(Long id);
}
