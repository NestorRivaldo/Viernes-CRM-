package com.viernes.clientes.adapters.out.external;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "viernes.documentos.apisperu")
@Getter
@Setter
public class ApisPeruProperties {

    private String baseUrl = "https://dniruc.apisperu.com/api/v1";
    private String token;
    private Duration timeout = Duration.ofSeconds(5);
}
