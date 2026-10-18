package com.viernes.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Viernes V1 API")
                        .description("CRM Hibrido de Gestion Comercial y Marketing")
                        .version("v1"))
                .servers(List.of(new Server().url("http://localhost:8080").description("Generated server url")));
    }
}
