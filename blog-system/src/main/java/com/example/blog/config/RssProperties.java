package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RSS 数据源抓取配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "rss")
public class RssProperties {
    private boolean enabled = true;
    private int timeoutMs = 15000;
    /** 单次请求之间的间隔，单位毫秒。 */
    private int requestIntervalMs = 800;
    private int topLimit = 100;
    private String defaultLanguage = "en";
    private List<String> feeds = new ArrayList<>();
}
