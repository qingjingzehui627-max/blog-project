package com.example.blog.dto;

import com.example.blog.entity.NewsArticle;
import lombok.Data;

/**
 * 新闻详情响应。
 */
@Data
public class NewsArticleDetailResponse {
    private NewsArticle article;
    private NewsAiSummaryResponse aiSummary;
}
