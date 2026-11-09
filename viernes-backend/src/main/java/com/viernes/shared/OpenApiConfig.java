package com.viernes.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Viernes V1 API")
                        .description("CRM Hibrido de Gestion Comercial y Marketing. "
                                + "Los endpoints /api/v1/** requieren un token JWT emitido por Keycloak "
                                + "(realm viernes-realm).")
                        .version("v1"))
                .servers(List.of(new Server().url("http://localhost:8080").description("Generated server url")))
                // Declara el esquema Bearer JWT para que Swagger UI muestre el candado
                // en cada operacion y el boton "Authorize".
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token de acceso de Keycloak (realm viernes-realm)")));
    }
}
