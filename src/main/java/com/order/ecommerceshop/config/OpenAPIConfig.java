package com.order.ecommerceshop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class    OpenAPIConfig
{
    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI myStoreOpenAPI()
    {
        return new OpenAPI()
                .info(new Info()
                        .title("MyStore API")
                        .description("E-Commerce REST APIs\n\n" +
                        "**How to authenticate:**\n" +
                        "1. Call `POST /api/rest/auth/login` with email/password\n" +
                        "2. Copy the `token` from the response\n" +
                        "3. Click the **Authorize** button (top-right)\n" +
                        "4. Paste the token (without 'Bearer' prefix if prompted, " +
                        "or with it — Swagger adds it automatically)\n" +
                        "5. Now all protected APIs will work")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME,new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token from /api/rest/auth/login")));
    }
}
