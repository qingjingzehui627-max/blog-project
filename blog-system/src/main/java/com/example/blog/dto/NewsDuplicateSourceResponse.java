package com.example.blog.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 重复新闻来源明细响应。
 */
@Data
public class NewsDuplicateSourceResponse {
    private Long id;
    private String providerCode;
    private String sourceContentId;
    private String title;
    private String sourceUrl;
    private LocalDateTime publishedAt;
    private LocalDate fetchDate;
}
