package com.example.picturebackend.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    /**
     * 对上传的文件做映射，存放到对应位置中
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:D:\\AAA项目学习\\spring项目\\智能云图库项目\\picture-backend\\avatarSave\\");
    }
}
