package com.nitrotech.api.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String COOKIE_SCHEME = "cookieAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nitrotech API")
                        .version("1.0.0")
                        .description("Nitrotech e-commerce REST API"))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("SESSION")
                                .description("Session cookie (Spring Session / Redis). Login via POST /api/auth/login first, then the browser will send this cookie automatically.")));
    }
}
