package com.example.blog.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * RSS 单条新闻结果。
 */
@Data
public class RssFeedItem {
    private String feedUrl;
    private String title;
    private String summary;
    private String url;
    private String author;
    private LocalDateTime publishedAt;
}
