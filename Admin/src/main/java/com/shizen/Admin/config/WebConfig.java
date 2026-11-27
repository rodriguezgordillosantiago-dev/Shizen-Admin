package com.shizen.Admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expone los archivos multimedia y documentos subidos en shizenhome
 * (logos de negocios, fotos de repartidores y archivos legales) bajo /shizen-media/**.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/shizen-media/**")
                .addResourceLocations("file:///C:/xampp/htdocs/shizenhome/");
    }
}
