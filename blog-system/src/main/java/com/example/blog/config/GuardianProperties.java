package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Guardian 新闻抓取模块配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "guardian")
public class GuardianProperties {
    private boolean enabled = true;
    private String baseUrl = "https://content.guardianapis.com";
    private String apiKey = "";
    private int timeoutMs = 20000;
    private int pageSize = 50;
    private int topLimit = 100;
    private int maxPagesPerQuery = 2;
    /** 单次请求之间的间隔，单位毫秒。 */
    private int requestIntervalMs = 1200;
    /** 定时任务 cron 表达式。 */
    private String cron = "0 10 6 * * ?";
    /** 定时任务执行时区。 */
    private String zone = "Asia/Shanghai";
    /** 抓取日期偏移量，-1 表示抓取前一天。 */
    private int fetchOffsetDays = -1;
    private String defaultCategoryCode = "GENERAL_TECH";
    private String defaultCategoryName = "科技综合";
    private List<String> sections = new ArrayList<>();
    private List<String> queries = new ArrayList<>();

    /**
     * 初始化默认抓取参数。
     */
    public GuardianProperties() {
        sections.add("technology");
        sections.add("science");
        sections.add("business");

        queries.add("artificial intelligence");
        queries.add("machine learning");
        queries.add("OpenAI");
        queries.add("Anthropic");
        queries.add("Nvidia");
        queries.add("technology");
    }
}
