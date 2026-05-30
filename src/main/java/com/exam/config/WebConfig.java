package com.exam.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    private final JwtInterceptor jwtInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(JwtInterceptor jwtInterceptor, @Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.jwtInterceptor = jwtInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }

            @Override
            public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                registry.addInterceptor(jwtInterceptor)
                        .addPathPatterns("/api/**")
                        .excludePathPatterns("/api/auth/login");
            }
        };
    }
}
