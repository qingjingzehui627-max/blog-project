package com.example.blog.client.provider.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一后的单条新闻来源数据。
 */
@Data
public class NewsProviderArticle {
    private String providerCode;
    private String sourceContentId;
    private String sectionId;
    private String sectionName;
    private String pillarId;
    private String pillarName;
    private String title;
    private String summary;
    private String content;
    private String author;
    private String webUrl;
    private String apiUrl;
    private String thumbnailUrl;
    private LocalDateTime publishedAt;
    private String lang;
    private String rawJson;
}
