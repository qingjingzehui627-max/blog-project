package com.example.blog.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 新闻 AI 摘要响应。
 */
@Data
public class NewsAiSummaryResponse {
    private Long id;
    private Long articleId;
    private LocalDate fetchDate;
    private String summaryText;
    private String summaryStatus;
    private String triggerType;
    private String modelName;
    private String promptVersion;
    private LocalDateTime generatedAt;
    private Integer retryCount;
    private String errorMessage;
}
