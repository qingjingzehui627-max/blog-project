package com.example.blog.client.provider;

import com.example.blog.client.CurrentsApiClient;
import com.example.blog.client.dto.CurrentsLatestNewsResponse;
import com.example.blog.client.provider.dto.NewsProviderArticle;
import com.example.blog.client.provider.dto.NewsProviderFetchResult;
import com.example.blog.config.CurrentsProperties;
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
 * Currents API 数据源适配器。
 */
@Component
public class CurrentsNewsProviderAdapter implements NewsProviderAdapter {

    private static final String PROVIDER_CODE = "currents";

    @Resource
    private CurrentsProperties currentsProperties;

    @Resource
    private CurrentsApiClient currentsApiClient;

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
        return "Currents API";
    }

    /**
     * 判断当前数据源是否启用。
     *
     * @return true 表示启用
     */
    @Override
    public boolean isEnabled() {
        return currentsProperties.isEnabled();
    }

    /**
     * 校验 Currents API 抓取配置。
     *
     * @return 返回 null 表示配置有效，否则返回错误信息
     */
    @Override
    public String validateConfig() {
        if (!currentsProperties.isEnabled()) {
            return "Currents API 新闻抓取未启用";
        }
        if (!StringUtils.hasText(currentsProperties.getApiKey())) {
            return "未配置 Currents API Key";
        }
        return null;
    }

    /**
     * 执行一次 Currents 新闻抓取。
     *
     * @param fetchDate 抓取日期
     * @return 数据源抓取结果
     */
    @Override
    public NewsProviderFetchResult fetchArticles(LocalDate fetchDate) {
        NewsProviderFetchResult fetchResult = new NewsProviderFetchResult();
        fetchResult.setProviderCode(PROVIDER_CODE);

        LinkedHashMap<String, NewsProviderArticle> uniqueArticles = new LinkedHashMap<>();
        List<String> keywords = currentsProperties.getKeywords() == null ? new ArrayList<>() : currentsProperties.getKeywords();

        for (String keyword : keywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }

            for (int page = 1; page <= currentsProperties.getMaxPagesPerQuery(); page++) {
                CurrentsLatestNewsResponse response = currentsApiClient.searchNews(fetchDate, keyword, page);
                fetchResult.setRequestCount(fetchResult.getRequestCount() + 1);

                List<CurrentsLatestNewsResponse.NewsItem> articles = response == null ? List.of() : response.getNews();
                if (articles == null || articles.isEmpty()) {
                    break;
                }

                for (CurrentsLatestNewsResponse.NewsItem item : articles) {
                    NewsProviderArticle article = toProviderArticle(item, fetchDate);
                    if (!StringUtils.hasText(article.getSourceContentId())) {
                        continue;
                    }
                    uniqueArticles.putIfAbsent(article.getSourceContentId(), article);
                }

                if (articles.size() < currentsProperties.getPageSize() || uniqueArticles.size() >= currentsProperties.getTopLimit()) {
                    break;
                }
                sleepQuietly(currentsProperties.getRequestIntervalMs());
            }

            if (uniqueArticles.size() >= currentsProperties.getTopLimit()) {
                break;
            }
        }

        fetchResult.setArticles(uniqueArticles.values().stream()
                .sorted((left, right) -> comparePublishedTime(right.getPublishedAt(), left.getPublishedAt()))
                .limit(currentsProperties.getTopLimit())
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
        config.put("pageSize", currentsProperties.getPageSize());
        config.put("topLimit", currentsProperties.getTopLimit());
        config.put("maxPagesPerQuery", currentsProperties.getMaxPagesPerQuery());
        config.put("requestIntervalMs", currentsProperties.getRequestIntervalMs());
        config.put("language", currentsProperties.getLanguage());
        config.put("category", currentsProperties.getCategory());
        config.put("keywords", currentsProperties.getKeywords());
        return config;
    }

    /**
     * 将 Currents 结果转换为统一来源模型。
     *
     * @param item Currents 单条结果
     * @param fetchDate 抓取日期
     * @return 统一来源模型
     */
    private NewsProviderArticle toProviderArticle(CurrentsLatestNewsResponse.NewsItem item, LocalDate fetchDate) {
        NewsProviderArticle article = new NewsProviderArticle();
        article.setProviderCode(PROVIDER_CODE);
        article.setSourceContentId(buildSourceContentId(item));
        article.setSectionName(item.getCategory() == null || item.getCategory().isEmpty()
                ? currentsProperties.getCategory()
                : String.join(",", item.getCategory()));
        article.setTitle(limit(item.getTitle(), 500));
        article.setSummary(limit(item.getDescription(), 5000));
        article.setAuthor(limit(item.getAuthor(), 255));
        article.setWebUrl(limit(item.getUrl(), 500));
        article.setThumbnailUrl(limit(item.getImage(), 500));
        article.setPublishedAt(parsePublishedAt(item.getPublished(), fetchDate));
        article.setLang(StringUtils.hasText(item.getLanguage()) ? item.getLanguage() : currentsProperties.getLanguage());
        article.setRawJson(toRawJson(item));
        return article;
    }

    /**
     * 构造来源内容 ID。
     *
     * @param item Currents 单条结果
     * @return 来源内容 ID
     */
    private String buildSourceContentId(CurrentsLatestNewsResponse.NewsItem item) {
        if (StringUtils.hasText(item.getId())) {
            return item.getId();
        }
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
     * @param item Currents 单条结果
     * @return JSON 字符串
     */
    private String toRawJson(CurrentsLatestNewsResponse.NewsItem item) {
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
            throw new IllegalStateException("计算 Currents 来源哈希失败", exception);
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
