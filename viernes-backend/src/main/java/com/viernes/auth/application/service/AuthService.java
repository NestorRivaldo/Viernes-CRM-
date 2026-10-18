package com.viernes.auth.application.service;

import com.viernes.auth.application.dto.LoginRequest;
import com.viernes.auth.application.dto.LoginResponse;
import com.viernes.auth.application.dto.UsuarioLoginResponse;
import com.viernes.usuarios.UsuarioResumen;
import com.viernes.usuarios.UsuariosApi;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ESTADO_INACTIVO = "INACTIVO";

    private final UsuariosApi usuariosApi;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        UsuarioResumen usuario = usuariosApi.buscarPorCorreo(request.correo())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.passwordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (ESTADO_INACTIVO.equals(usuario.estado())) {
            throw new DisabledException("El usuario está desactivado");
        }

        UserDetails userDetails = User.builder()
                .username(usuario.correo())
                .password(usuario.passwordHash())
                .roles(usuario.rol())
                .build();

        String token = jwtService.generateToken(userDetails);

        UsuarioLoginResponse usuarioResponse = new UsuarioLoginResponse(
                usuario.id(), usuario.nombre(), usuario.apellido(),
                usuario.correo(), usuario.rol(), usuario.estado(), usuario.creadoEn()
        );

        return new LoginResponse(token, "Bearer", jwtService.getExpiration(), usuarioResponse);
    }
}
