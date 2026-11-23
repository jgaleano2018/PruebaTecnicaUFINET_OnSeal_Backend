package com.ufinet.offers.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI offersOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ufinet - API de Ofertas")
                .version("1.0.0")
                .description("Crea ofertas, calcula su MRC y aplica la regla de presentación. "
                        + "Los errores se devuelven como ProblemDetail (RFC 9457).")
                .contact(new Contact().name("Equipo Backend Ufinet")));
    }
}
