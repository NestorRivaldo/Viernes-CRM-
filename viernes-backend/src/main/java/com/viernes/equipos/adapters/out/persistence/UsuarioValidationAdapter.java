package com.viernes.equipos.adapters.out.persistence;

import com.viernes.equipos.application.port.out.UsuarioValidationPort;
import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.UsuariosApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * Adaptador que implementa la validación de existencia de usuarios
 * consumiendo la API pública del módulo Usuarios (named interface),
 * sin acceder a su infraestructura interna.
 */
@Component
@RequiredArgsConstructor
public class UsuarioValidationAdapter implements UsuarioValidationPort {

    private final UsuariosApi usuariosApi;

    @Override
    public boolean existeUsuario(Long usuarioId) {
        return usuariosApi.existePorId(usuarioId);
    }

    @Override
    public Optional<UsuarioResumen> buscarUsuario(Long usuarioId) {
        return usuariosApi.buscarPorId(usuarioId);
    }
}
