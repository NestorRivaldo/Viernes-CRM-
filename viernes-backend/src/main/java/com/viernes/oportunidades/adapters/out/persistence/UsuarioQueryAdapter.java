package com.viernes.oportunidades.adapters.out.persistence;

import com.viernes.oportunidades.application.port.out.UsuarioQueryPort;
import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.UsuariosApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class UsuarioQueryAdapter implements UsuarioQueryPort {

    private final UsuariosApi usuariosApi;

    public UsuarioQueryAdapter(UsuariosApi usuariosApi) {
        this.usuariosApi = usuariosApi;
    }

    @Override
    public Optional<UsuarioResumen> buscarPorId(Long idUsuario) {
        return usuariosApi.buscarPorId(idUsuario);
    }
}
