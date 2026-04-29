package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 聚合新闻主表实体。
 */
@Data
@TableName("news_articles")
public class NewsArticle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String source;
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
    private LocalDate fetchDate;
    private Integer rankOrder;
    private String categoryCode;
    private String categoryName;
    private String keywordTags;
    private String lang;
    private Integer status;
    private String rawJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
