package com.example.blog.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 重复新闻聚合结果响应。
 */
@Data
public class NewsDuplicateArticleResponse {
    private Long articleId;
    private String title;
    private String webUrl;
    private String categoryCode;
    private String categoryName;
    private String primaryProvider;
    private Integer sourceCount;
    private LocalDate fetchDate;
    private LocalDateTime publishedAt;
    private List<NewsDuplicateSourceResponse> sources = new ArrayList<>();
}
