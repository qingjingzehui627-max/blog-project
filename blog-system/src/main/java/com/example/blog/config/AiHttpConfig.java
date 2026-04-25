package com.example.blog.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;

@Configuration
public class AiHttpConfig {

    @Resource
    private AiProperties aiProperties;

    @Bean
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder) {
        Duration timeout = Duration.ofMillis(aiProperties.getTimeoutMs());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }
}
