package com.example.picturebackend.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pixoraOpenApi() {
        // 使用标准 OpenAPI 配置，避免绑定到 Knife4j 的旧版 Springdoc 扩展。
        return new OpenAPI().info(new Info()
                .title("PIXORA API")
                .description("PIXORA 智能云图库")
                .version("v2.0"));
    }
}
