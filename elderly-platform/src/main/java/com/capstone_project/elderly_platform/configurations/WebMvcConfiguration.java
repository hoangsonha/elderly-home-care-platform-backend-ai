package com.capstone_project.elderly_platform.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC Configuration to support multipart/form-data with JSON parts
 * This allows Swagger UI to send application/octet-stream for JSON parts
 * and Spring will still be able to deserialize them correctly.
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Add a custom converter that can handle application/octet-stream for JSON
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                new MediaType("application", "octet-stream"), // Support octet-stream for Swagger
                MediaType.APPLICATION_OCTET_STREAM
        ));
        converters.add(jsonConverter);
    }
}

