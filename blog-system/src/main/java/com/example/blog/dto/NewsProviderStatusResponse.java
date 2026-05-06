package com.example.blog.dto;

import lombok.Data;

import java.util.Map;

/**
 * 新闻数据源状态响应。
 */
@Data
public class NewsProviderStatusResponse {
    private String providerCode;
    private String displayName;
    private boolean enabled;
    private boolean configValid;
    private String message;
    private Map<String, Object> config;
}
