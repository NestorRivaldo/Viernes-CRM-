package com.viernes.oportunidades.application.port.in;

import com.viernes.oportunidades.application.dto.OportunidadColaboradorRequest;
import com.viernes.oportunidades.application.dto.OportunidadColaboradorResponse;
import com.viernes.oportunidades.application.dto.OportunidadContactoRequest;
import com.viernes.oportunidades.application.dto.OportunidadContactoResponse;
import com.viernes.oportunidades.application.dto.OportunidadContactoUpdateRequest;
import com.viernes.oportunidades.application.dto.OportunidadProductoRequest;
import com.viernes.oportunidades.application.dto.OportunidadProductoResponse;
import com.viernes.oportunidades.application.dto.OportunidadRequest;
import com.viernes.oportunidades.application.dto.OportunidadResponse;
import com.viernes.oportunidades.domain.EtapaOportunidad;
import java.util.List;

public interface ManageOportunidadUseCase {
    List<OportunidadResponse> listar();
    List<OportunidadResponse> buscar(String q);
    List<OportunidadResponse> filtrarPorEtapa(EtapaOportunidad etapa);
    OportunidadResponse obtener(Long id);
    OportunidadResponse crear(OportunidadRequest request);
    OportunidadResponse actualizar(Long id, OportunidadRequest request);
    OportunidadResponse cambiarEtapa(Long id, EtapaOportunidad etapa);
    void eliminar(Long id);

    List<OportunidadContactoResponse> listarContactos(Long id);
    OportunidadContactoResponse agregarContacto(Long id, OportunidadContactoRequest request);
    OportunidadContactoResponse actualizarContacto(Long id, Long contactoId, OportunidadContactoUpdateRequest request);
    void quitarContacto(Long id, Long contactoId);

    List<OportunidadProductoResponse> listarProductos(Long id);
    OportunidadProductoResponse agregarProducto(Long id, OportunidadProductoRequest request);
    void quitarProducto(Long id, Long productoId);

    List<OportunidadColaboradorResponse> listarColaboradores(Long id);
    OportunidadColaboradorResponse agregarColaborador(Long id, OportunidadColaboradorRequest request);
    void quitarColaborador(Long id, Long usuarioId);
}
