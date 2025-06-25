package it.polimi.gpplib.eforms_gpp_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Autowired
    private AppConfig appConfig;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(appConfig.getCors().getAllowedOrigins().toArray(new String[0]))
                        .allowedMethods(appConfig.getCors().getAllowedMethods().toArray(new String[0]))
                        .allowedHeaders(appConfig.getCors().getAllowedHeaders().toArray(new String[0]));
            }
        };
    }
}
