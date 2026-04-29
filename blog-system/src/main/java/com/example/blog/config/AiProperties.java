package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 模块配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private boolean enabled = false;
    private String provider = "openai";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-4.1-mini";
    private String systemPrompt = "你是博客网站的 AI 助手，请用简洁、准确、友好的方式回答用户问题。";
    private double temperature = 0.7D;
    private int dailyLimit = 10;
    private int maxHistory = 10;
    private int timeoutMs = 30000;
    private List<String> models = new ArrayList<>();
}
