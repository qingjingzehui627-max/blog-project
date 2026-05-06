package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * NewsAPI 抓取配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "newsapi")
public class NewsApiProperties {
    private boolean enabled = true;
    private String baseUrl = "https://newsapi.org/v2";
    private String apiKey = "";
    private int timeoutMs = 20000;
    private int pageSize = 100;
    private int topLimit = 100;
    private int maxPagesPerQuery = 1;
    /** 单次请求之间的间隔，单位毫秒。 */
    private int requestIntervalMs = 1200;
    private String language = "en";
    private String sortBy = "publishedAt";
    private List<String> domains = new ArrayList<>();
    private List<String> queries = new ArrayList<>();
}
