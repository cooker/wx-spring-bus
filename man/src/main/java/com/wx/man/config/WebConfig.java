package com.wx.man.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS：允许前端 man-vue（默认 localhost:5173）访问 man API。
 * 静态资源：打包后的 man-vue 放在 classpath:/static/，SPA 路由回退到 index.html。
 */
@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer(
        @Value("${man.cors.allowed-origin:http://localhost:5173}") String allowedOrigin
    ) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(allowedOrigin)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/")
                    .resourceChain(true)
                    .addResolver(new SpaIndexResourceResolver());
            }
        };
    }
}
