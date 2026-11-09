package com.viernes.mensajeria.application.port.in;

import com.viernes.mensajeria.application.dto.MensajeRequest;
import com.viernes.mensajeria.application.dto.MensajeResponse;
import com.viernes.mensajeria.application.dto.ResumenMensajesResponse;

import java.util.List;

public interface ManageMensajeUseCase {

    List<MensajeResponse> listar(Long despuesDeId, int limite);

    ResumenMensajesResponse resumen(Long despuesDeId);

    MensajeResponse enviar(String usuarioCorreo, String usuarioNombre, MensajeRequest request);
}
