package com.example.blog.client.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Currents API 查询响应。
 */
@Data
public class CurrentsLatestNewsResponse {
    private String status;
    private List<NewsItem> news = new ArrayList<>();

    /**
     * Currents 新闻条目。
     */
    @Data
    public static class NewsItem {
        private String id;
        private String title;
        private String description;
        private String url;
        private String author;
        private String image;
        private String language;
        private List<String> category;
        private String published;
    }
}
