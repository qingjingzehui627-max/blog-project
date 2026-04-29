package com.example.blog.dto;

import lombok.Data;

/**
 * 新闻分类规则更新请求。
 */
@Data
public class NewsRuleUpdateRequest {
    private String categoryCode;
    private String categoryName;
    private String includeKeywords;
    private String excludeKeywords;
    private Integer priority;
    private Integer enabled;
}
