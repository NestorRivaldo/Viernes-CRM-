package com.viernes.auth.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración de Seguridad OAuth2 Resource Server para Keycloak.
 * Solo se activa cuando el perfil 'keycloak' está presente:
 * java -jar app.jar --spring.profiles.active=dev,keycloak
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("keycloak")
public class KeycloakSecurityConfig {

    /**
     * Catálogos y estructura comercial que solo ADMIN y JEFE pueden modificar.
     * La lectura sigue disponible para cualquier usuario autenticado.
     */
    private static final String[] GESTION_COMERCIAL = {
            "/api/v1/productos/**",
            "/api/v1/campanias/**",
            "/api/v1/fuentes/**",
            "/api/v1/equipos/**",
            "/api/v1/etiquetas/**"
    };

    /**
     * Entidades de negocio transaccional cuya eliminación física o permanente
     * queda reservada estrictamente a usuarios con rol ADMIN.
     */
    private static final String[] ENTIDADES_NEGOCIO = {
            "/api/v1/clientes/**",
            "/api/v1/leads/**",
            "/api/v1/oportunidades/**",
            "/api/v1/propuestas/**",
            "/api/v1/actividades/**"
    };

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KeycloakSecurityConfig(KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter) {
        this.keycloakJwtAuthenticationConverter = keycloakJwtAuthenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${viernes.cors.allowed-origins:http://localhost:4200,http://localhost:8080}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain keycloakSecurityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    Map<String, Object> body = new HashMap<>();
                    body.put("timestamp", Instant.now().toString());
                    body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
                    body.put("error", "Unauthorized");
                    body.put("message", "Acceso no autorizado: se requiere un token Bearer JWT válido de Keycloak");
                    body.put("path", request.getRequestURI());
                    objectMapper.writeValue(response.getOutputStream(), body);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    Map<String, Object> body = new HashMap<>();
                    body.put("timestamp", Instant.now().toString());
                    body.put("status", HttpServletResponse.SC_FORBIDDEN);
                    body.put("error", "Forbidden");
                    body.put("message", "Acceso denegado: no cuenta con los roles requeridos para ejecutar esta operación");
                    body.put("path", request.getRequestURI());
                    objectMapper.writeValue(response.getOutputStream(), body);
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/static/**", "/", "/index.html").permitAll()

                // Gestión de usuarios — solo ADMIN para escritura
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/usuarios/**").hasRole("ADMIN")

                // Transiciones críticas de propuestas — solo ADMIN y JEFE pueden aprobar, rechazar o cancelar
                .requestMatchers(HttpMethod.PATCH, "/api/v1/propuestas/*/aprobar").hasAnyRole("ADMIN", "JEFE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/propuestas/*/rechazar").hasAnyRole("ADMIN", "JEFE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/propuestas/*/cancelar").hasAnyRole("ADMIN", "JEFE")

                // Eliminación de registros de negocio — solo ADMIN
                .requestMatchers(HttpMethod.DELETE, ENTIDADES_NEGOCIO).hasRole("ADMIN")

                // Catálogos y estructura comercial — escritura solo ADMIN y JEFE
                .requestMatchers(HttpMethod.POST, GESTION_COMERCIAL).hasAnyRole("ADMIN", "JEFE")
                .requestMatchers(HttpMethod.PUT, GESTION_COMERCIAL).hasAnyRole("ADMIN", "JEFE")
                .requestMatchers(HttpMethod.PATCH, GESTION_COMERCIAL).hasAnyRole("ADMIN", "JEFE")
                .requestMatchers(HttpMethod.DELETE, GESTION_COMERCIAL).hasAnyRole("ADMIN", "JEFE")

                // Todo lo demás requiere token válido de Keycloak (VENDEDOR, JEFE, ADMIN)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
            );

        return http.build();
    }
}
