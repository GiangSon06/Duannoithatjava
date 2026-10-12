package vn.sland.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class StaticUploadConfig implements WebMvcConfigurer {
    private final String uploadDir;
    public StaticUploadConfig(@Value("${app.upload-dir:uploads}") String uploadDir) { this.uploadDir = uploadDir; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uri = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(uri);
    }
}
