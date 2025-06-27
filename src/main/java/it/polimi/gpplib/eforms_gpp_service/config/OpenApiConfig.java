package it.polimi.gpplib.eforms_gpp_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
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
                        .title("eForms GPP Service API")
                        .description(
                                "Service to expose the functionality of the eForms GPP (Green Public Procurement) library. "
                                        +
                                        "This API provides endpoints for analyzing procurement notices, suggesting GPP patches, "
                                        +
                                        "applying patches, visualizing notices, and validating notices.")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("Politecnico di Milano")
                                .email("support@polimi.it"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("https://api.example.com")
                                .description("Production server")));
    }
}
