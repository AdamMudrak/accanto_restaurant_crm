package com.example.restaurant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Value("${server.path}")
    private String serverPath;

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Restaurant API")
                        .description("Restaurant guest ordering, authentication, menu and "
                    + "table management, "
                                + "payments, refunds, reviews, scheduling, and analytics. "
                                + "CRM routes use the accessToken cookie; guest order and payment "
                                + "flows use guestUuid and orderUuid cookies from QR entry.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(
                                "accessTokenCookie",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("accessToken")
                                        .description(
                                                "JWT access token stored in an HttpOnly cookie.")))
                .addServersItem(new Server().url(serverPath));
    }
}
