package com.example.blog.client.provider;

import com.example.blog.client.NewsApiClient;
import com.example.blog.client.dto.NewsApiEverythingResponse;
import com.example.blog.client.provider.dto.NewsProviderArticle;
import com.example.blog.client.provider.dto.NewsProviderFetchResult;
import com.example.blog.config.NewsApiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * NewsAPI 数据源适配器。
 */
@Component
public class NewsApiNewsProviderAdapter implements NewsProviderAdapter {

    private static final String PROVIDER_CODE = "newsapi";

    @Resource
    private NewsApiProperties newsApiProperties;

    @Resource
    private NewsApiClient newsApiClient;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 返回数据源编码。
     *
     * @return 数据源编码
     */
    @Override
    public String getProviderCode() {
        return PROVIDER_CODE;
    }

    /**
     * 返回数据源展示名称。
     *
     * @return 数据源展示名称
     */
    @Override
    public String getDisplayName() {
        return "NewsAPI";
    }

    /**
     * 判断当前数据源是否启用。
     *
     * @return true 表示启用
     */
    @Override
    public boolean isEnabled() {
        return newsApiProperties.isEnabled();
    }

    /**
     * 校验 NewsAPI 抓取配置。
     *
     * @return 返回 null 表示配置有效，否则返回错误信息
     */
    @Override
    public String validateConfig() {
        if (!newsApiProperties.isEnabled()) {
            return "NewsAPI 新闻抓取未启用";
        }
        if (!StringUtils.hasText(newsApiProperties.getApiKey())) {
            return "未配置 NewsAPI Key";
        }
        return null;
    }

    /**
     * 执行一次 NewsAPI 新闻抓取。
     *
     * @param fetchDate 抓取日期
     * @return 数据源抓取结果
     */
    @Override
    public NewsProviderFetchResult fetchArticles(LocalDate fetchDate) {
        NewsProviderFetchResult fetchResult = new NewsProviderFetchResult();
        fetchResult.setProviderCode(PROVIDER_CODE);

        LinkedHashMap<String, NewsProviderArticle> uniqueArticles = new LinkedHashMap<>();
        List<String> queries = newsApiProperties.getQueries() == null ? new ArrayList<>() : newsApiProperties.getQueries();

        for (String query : queries) {
            if (!StringUtils.hasText(query)) {
                continue;
            }
            for (int page = 1; page <= newsApiProperties.getMaxPagesPerQuery(); page++) {
                NewsApiEverythingResponse response = newsApiClient.searchNews(fetchDate, query, page);
                fetchResult.setRequestCount(fetchResult.getRequestCount() + 1);

                List<NewsApiEverythingResponse.ArticleItem> articles = response == null ? List.of() : response.getArticles();
                if (articles == null || articles.isEmpty()) {
                    break;
                }

                for (NewsApiEverythingResponse.ArticleItem item : articles) {
                    NewsProviderArticle article = toProviderArticle(item, fetchDate);
                    if (!StringUtils.hasText(article.getSourceContentId())) {
                        continue;
                    }
                    uniqueArticles.putIfAbsent(article.getSourceContentId(), article);
                }

                if (articles.size() < newsApiProperties.getPageSize() || uniqueArticles.size() >= newsApiProperties.getTopLimit()) {
                    break;
                }
                sleepQuietly(newsApiProperties.getRequestIntervalMs());
            }

            if (uniqueArticles.size() >= newsApiProperties.getTopLimit()) {
                break;
            }
        }

        fetchResult.setArticles(uniqueArticles.values().stream()
                .sorted((left, right) -> comparePublishedTime(right.getPublishedAt(), left.getPublishedAt()))
                .limit(newsApiProperties.getTopLimit())
                .toList());
        return fetchResult;
    }

    /**
     * 返回后台展示所需的配置摘要。
     *
     * @return 配置摘要
     */
    @Override
    public Map<String, Object> getAdminConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("pageSize", newsApiProperties.getPageSize());
        config.put("topLimit", newsApiProperties.getTopLimit());
        config.put("maxPagesPerQuery", newsApiProperties.getMaxPagesPerQuery());
        config.put("requestIntervalMs", newsApiProperties.getRequestIntervalMs());
        config.put("language", newsApiProperties.getLanguage());
        config.put("domains", newsApiProperties.getDomains());
        config.put("queries", newsApiProperties.getQueries());
        return config;
    }

    /**
     * 将 NewsAPI 结果转换为统一来源模型。
     *
     * @param item NewsAPI 单条结果
     * @param fetchDate 抓取日期
     * @return 统一来源模型
     */
    private NewsProviderArticle toProviderArticle(NewsApiEverythingResponse.ArticleItem item, LocalDate fetchDate) {
        NewsProviderArticle article = new NewsProviderArticle();
        article.setProviderCode(PROVIDER_CODE);
        article.setSourceContentId(buildSourceContentId(item));
        article.setSectionId(item.getSource() == null ? null : item.getSource().getId());
        article.setSectionName(item.getSource() == null ? null : item.getSource().getName());
        article.setTitle(limit(item.getTitle(), 500));
        article.setSummary(limit(item.getDescription(), 5000));
        article.setContent(item.getContent());
        article.setAuthor(limit(item.getAuthor(), 255));
        article.setWebUrl(limit(item.getUrl(), 500));
        article.setThumbnailUrl(limit(item.getUrlToImage(), 500));
        article.setPublishedAt(parsePublishedAt(item.getPublishedAt(), fetchDate));
        article.setLang(newsApiProperties.getLanguage());
        article.setRawJson(toRawJson(item));
        return article;
    }

    /**
     * 构造来源内容 ID。
     *
     * @param item NewsAPI 单条结果
     * @return 来源内容 ID
     */
    private String buildSourceContentId(NewsApiEverythingResponse.ArticleItem item) {
        if (StringUtils.hasText(item.getUrl())) {
            return sha256(item.getUrl());
        }
        if (StringUtils.hasText(item.getTitle())) {
            return sha256(item.getTitle());
        }
        return "";
    }

    /**
     * 解析发布时间。
     *
     * @param value 原始发布时间
     * @param fallbackDate 兜底日期
     * @return 解析后的发布时间
     */
    private LocalDateTime parsePublishedAt(String value, LocalDate fallbackDate) {
        try {
            return StringUtils.hasText(value)
                    ? OffsetDateTime.parse(value).toLocalDateTime()
                    : fallbackDate.atStartOfDay();
        } catch (Exception exception) {
            return fallbackDate.atStartOfDay();
        }
    }

    /**
     * 序列化来源原始 JSON。
     *
     * @param item NewsAPI 单条结果
     * @return JSON 字符串
     */
    private String toRawJson(NewsApiEverythingResponse.ArticleItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    /**
     * 比较发布时间。
     *
     * @param left 左时间
     * @param right 右时间
     * @return 比较结果
     */
    private int comparePublishedTime(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    /**
     * 计算 SHA-256。
     *
     * @param value 原始值
     * @return 哈希值
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("计算 NewsAPI 来源哈希失败", exception);
        }
    }

    /**
     * 按配置做请求节流。
     *
     * @param requestIntervalMs 间隔毫秒数
     */
    private void sleepQuietly(int requestIntervalMs) {
        try {
            TimeUnit.MILLISECONDS.sleep(Math.max(requestIntervalMs, 0));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 截断超长字符串。
     *
     * @param value 原始值
     * @param maxLength 最大长度
     * @return 截断后的值
     */
    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
