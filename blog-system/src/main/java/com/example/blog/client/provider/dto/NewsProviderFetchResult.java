package com.example.blog.client.provider.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个数据源的一次抓取结果。
 */
@Data
public class NewsProviderFetchResult {
    private String providerCode;
    private int requestCount;
    private List<NewsProviderArticle> articles = new ArrayList<>();
}
