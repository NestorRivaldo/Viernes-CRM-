package com.viernes.usuarios.application.port.in;

import com.viernes.usuarios.application.dto.UsuarioRequest;
import com.viernes.usuarios.application.dto.UsuarioResponse;
import java.util.List;

public interface ManageUsuarioUseCase {
    List<UsuarioResponse> listar();
    UsuarioResponse obtener(Long id);
    UsuarioResponse obtenerPorCorreo(String correo);
    UsuarioResponse crear(UsuarioRequest request);
    UsuarioResponse actualizar(Long id, UsuarioRequest request);
    void activar(Long id);
    void desactivar(Long id);
}
