package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 新闻来源明细表实体。
 */
@Data
@TableName("news_article_sources")
public class NewsArticleSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String providerCode;
    private String sourceContentId;
    private String sourceUrl;
    private String normalizedUrl;
    private String urlHash;
    private String title;
    private String normalizedTitle;
    private String titleHash;
    private LocalDateTime publishedAt;
    private LocalDate fetchDate;
    private String rawJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
