package com.example.blog.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * Guardian 新闻模块 HTTP 客户端配置。
 */
@Configuration
public class GuardianHttpConfig {

    @Resource
    private GuardianProperties guardianProperties;

    /**
     * 创建 Guardian API 专用 RestTemplate。
     *
     * @param builder Spring 提供的构建器
     * @return 配置好超时时间的 RestTemplate
     */
    @Bean
    public RestTemplate guardianRestTemplate(RestTemplateBuilder builder) {
        Duration timeout = Duration.ofMillis(guardianProperties.getTimeoutMs());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }
}
