package com.viernes.mensajeria.application.service;

import com.viernes.mensajeria.application.dto.MensajeRequest;
import com.viernes.mensajeria.application.dto.MensajeResponse;
import com.viernes.mensajeria.application.dto.ResumenMensajesResponse;
import com.viernes.mensajeria.application.port.in.ManageMensajeUseCase;
import com.viernes.mensajeria.application.port.out.MensajePersistencePort;
import com.viernes.mensajeria.domain.Mensaje;
import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.UsuariosApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MensajeService implements ManageMensajeUseCase {

    private static final int LIMITE_MAX = 100;

    private final MensajePersistencePort persistencePort;
    private final UsuariosApi usuariosApi;

    @Override
    @Transactional(readOnly = true)
    public List<MensajeResponse> listar(Long despuesDeId, int limite) {
        int limiteSeguro = Math.min(Math.max(limite, 1), LIMITE_MAX);
        List<Mensaje> mensajes = despuesDeId == null
                ? persistencePort.findRecientes(limiteSeguro)
                : persistencePort.findDespuesDeId(despuesDeId, limiteSeguro);

        return mensajes.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenMensajesResponse resumen(Long despuesDeId) {
        long total = despuesDeId == null
                ? persistencePort.count()
                : persistencePort.countDespuesDeId(despuesDeId);

        return new ResumenMensajesResponse(total, persistencePort.findUltimoId());
    }

    @Override
    @Transactional
    public MensajeResponse enviar(String usuarioCorreo, String usuarioNombre, MensajeRequest request) {
        if (usuarioCorreo == null || usuarioCorreo.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al usuario autenticado");
        }

        String contenido = request.contenido() == null ? "" : request.contenido().trim();
        if (contenido.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        if (contenido.length() > 2000) {
            throw new IllegalArgumentException("El mensaje no puede superar los 2000 caracteres");
        }

        UsuarioResumen usuario = usuariosApi.buscarPorCorreo(usuarioCorreo).orElse(null);
        String correo = usuario != null && usuario.correo() != null && !usuario.correo().isBlank()
                ? usuario.correo()
                : usuarioCorreo;
        String nombre = usuarioNombre != null && !usuarioNombre.isBlank()
                ? usuarioNombre.trim()
                : nombreVisible(usuario, correo);

        Mensaje mensaje = Mensaje.builder()
                .usuarioCorreo(correo)
                .usuarioNombre(nombre)
                .contenido(contenido)
                .creadoEn(LocalDateTime.now())
                .build();

        return toResponse(persistencePort.save(mensaje));
    }

    private String nombreVisible(UsuarioResumen usuario, String correo) {
        if (usuario == null) {
            return correo;
        }
        String nombre = String.join(" ",
                usuario.nombre() == null ? "" : usuario.nombre().trim(),
                usuario.apellido() == null ? "" : usuario.apellido().trim()
        ).trim();
        return nombre.isBlank() ? correo : nombre;
    }

    private MensajeResponse toResponse(Mensaje mensaje) {
        return new MensajeResponse(
                mensaje.getId(),
                mensaje.getUsuarioCorreo(),
                mensaje.getUsuarioNombre(),
                mensaje.getContenido(),
                mensaje.getCreadoEn()
        );
    }
}
