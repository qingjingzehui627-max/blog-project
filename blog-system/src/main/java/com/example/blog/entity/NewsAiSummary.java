package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 新闻 AI 摘要表实体。
 */
@Data
@TableName("news_ai_summary")
public class NewsAiSummary {
    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
