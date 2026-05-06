package com.example.blog.client.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * NewsAPI 查询响应。
 */
@Data
public class NewsApiEverythingResponse {
    private String status;
    private Integer totalResults;
    private List<ArticleItem> articles = new ArrayList<>();

    /**
     * NewsAPI 新闻条目。
     */
    @Data
    public static class ArticleItem {
        private SourceItem source;
        private String author;
        private String title;
        private String description;
        private String url;
        private String urlToImage;
        private String publishedAt;
        private String content;
    }

    /**
     * NewsAPI 来源信息。
     */
    @Data
    public static class SourceItem {
        private String id;
        private String name;
    }
}
