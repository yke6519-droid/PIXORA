package com.example.picturebackend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局跨域配置
 * @return
 */
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter(){
        /**
         * 先做配置
         */
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
//        corsConfiguration.addAllowedOrigin("http://localhost:5173");
//        corsConfiguration.addAllowedOrigin("http://localhost:5174");
//        corsConfiguration.addAllowedOrigin("http://localhost:8088");
        corsConfiguration.addAllowedOriginPattern("*");
        /**
         * 再放入source中指定生效的路径
         */
        UrlBasedCorsConfigurationSource source=  new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",corsConfiguration);
        /**
         * 最后返回带有source的CorsFilter
         */
        return new CorsFilter(source);
    }
}