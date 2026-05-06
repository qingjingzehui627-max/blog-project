package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Currents API 抓取配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "currents")
public class CurrentsProperties {
    private boolean enabled = true;
    private String baseUrl = "https://api.currentsapi.services/v1";
    private String apiKey = "";
    private int timeoutMs = 20000;
    private int pageSize = 30;
    private int topLimit = 100;
    private int maxPagesPerQuery = 2;
    /** 单次请求之间的间隔，单位毫秒。 */
    private int requestIntervalMs = 1200;
    private String language = "en";
    private String category = "science_technology";
    private List<String> keywords = new ArrayList<>();
}
