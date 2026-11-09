package com.viernes.oportunidades.application.port.out;

import com.viernes.usuarios.UsuarioResumen;
import java.util.Optional;

public interface UsuarioQueryPort {

    Optional<UsuarioResumen> buscarPorId(Long idUsuario);
}
