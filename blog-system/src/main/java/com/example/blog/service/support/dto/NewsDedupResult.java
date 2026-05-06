package com.example.blog.service.support.dto;

import com.example.blog.entity.NewsArticle;
import com.example.blog.entity.NewsArticleSource;
import lombok.Data;

/**
 * 新闻去重匹配结果。
 */
@Data
public class NewsDedupResult {
    private boolean matched;
    private String stage;
    private double score;
    private NewsArticle article;
    private NewsArticleSource sourceRecord;
}
