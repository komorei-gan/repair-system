package com.repair.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

/**
 * Web MVC 配置：将 /uploads/** 映射到本地 uploads 目录，供浏览器直接访问上传的图片。
 * 鉴权放行见 SecurityConfig（/uploads/** permitAll）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadLocation;

    public WebConfig(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        // 归一化为绝对路径，末尾补分隔符，符合 ResourceHandler 对目录的约定
        String absolute = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        this.uploadLocation = absolute.endsWith(File.separator) ? absolute : absolute + File.separator;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadLocation);
    }
}
