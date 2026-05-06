package com.example.blog.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;

/**
 * 外部新闻数据源 HTTP 客户端配置。
 */
@Configuration
public class ExternalNewsHttpConfig {

    @Resource
    private NewsApiProperties newsApiProperties;

    @Resource
    private CurrentsProperties currentsProperties;

    @Resource
    private RssProperties rssProperties;

    /**
     * 创建 NewsAPI 专用 RestTemplate。
     *
     * @param builder Spring 提供的构建器
     * @return RestTemplate
     */
    @Bean
    public RestTemplate newsApiRestTemplate(RestTemplateBuilder builder) {
        Duration timeout = Duration.ofMillis(Math.max(newsApiProperties.getTimeoutMs(), 45000));
        return builder.setConnectTimeout(timeout).setReadTimeout(timeout).build();
    }

    /**
     * 创建 Currents API 专用 RestTemplate。
     *
     * @param builder Spring 提供的构建器
     * @return RestTemplate
     */
    @Bean
    public RestTemplate currentsRestTemplate(RestTemplateBuilder builder) {
        Duration timeout = Duration.ofMillis(currentsProperties.getTimeoutMs());
        return builder.setConnectTimeout(timeout).setReadTimeout(timeout).build();
    }

    /**
     * 创建 RSS 专用 RestTemplate。
     *
     * @param builder Spring 提供的构建器
     * @return RestTemplate
     */
    @Bean
    public RestTemplate rssRestTemplate(RestTemplateBuilder builder) {
        Duration timeout = Duration.ofMillis(rssProperties.getTimeoutMs());
        return builder.setConnectTimeout(timeout).setReadTimeout(timeout).build();
    }
}
