package com.example.blog.client.provider;

import com.example.blog.client.GuardianApiClient;
import com.example.blog.client.dto.GuardianSearchResponse;
import com.example.blog.client.provider.dto.NewsProviderArticle;
import com.example.blog.client.provider.dto.NewsProviderFetchResult;
import com.example.blog.config.GuardianProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
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
 * Guardian 数据源适配器。
 */
@Component
public class GuardianNewsProviderAdapter implements NewsProviderAdapter {

    private static final String PROVIDER_CODE = "guardian";

    @Resource
    private GuardianProperties guardianProperties;

    @Resource
    private GuardianApiClient guardianApiClient;

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
        return "Guardian";
    }

    /**
     * 判断当前数据源是否启用。
     *
     * @return true 表示启用
     */
    @Override
    public boolean isEnabled() {
        return guardianProperties.isEnabled();
    }

    /**
     * 校验 Guardian 抓取配置。
     *
     * @return 返回 null 表示配置有效，否则返回错误信息
     */
    @Override
    public String validateConfig() {
        if (!guardianProperties.isEnabled()) {
            return "Guardian 新闻抓取未启用";
        }
        if (!StringUtils.hasText(guardianProperties.getApiKey())) {
            return "未配置 Guardian API Key";
        }
        return null;
    }

    /**
     * 执行一次 Guardian 新闻抓取。
     *
     * @param fetchDate 抓取日期
     * @return 数据源抓取结果
     */
    @Override
    public NewsProviderFetchResult fetchArticles(LocalDate fetchDate) {
        NewsProviderFetchResult fetchResult = new NewsProviderFetchResult();
        fetchResult.setProviderCode(PROVIDER_CODE);

        LinkedHashMap<String, NewsProviderArticle> uniqueArticles = new LinkedHashMap<>();
        List<String> queries = guardianProperties.getQueries() == null
                ? new ArrayList<>()
                : guardianProperties.getQueries();

        // Guardian 会按多个关键词分别查询，这里先按来源内容 ID 去重，
        // 避免同一篇新闻命中多个关键词后重复进入后续聚合流程。
        for (String query : queries) {
            if (!StringUtils.hasText(query)) {
                continue;
            }

            for (int page = 1; page <= guardianProperties.getMaxPagesPerQuery(); page++) {
                GuardianSearchResponse response = guardianApiClient.searchNews(fetchDate, query, page);
                fetchResult.setRequestCount(fetchResult.getRequestCount() + 1);

                List<GuardianSearchResponse.ResultItem> results = response != null && response.getResponse() != null
                        ? response.getResponse().getResults()
                        : null;
                if (results == null || results.isEmpty()) {
                    break;
                }

                for (GuardianSearchResponse.ResultItem item : results) {
                    if (item == null || !StringUtils.hasText(item.getId())) {
                        continue;
                    }
                    uniqueArticles.putIfAbsent(item.getId(), toProviderArticle(item, fetchDate));
                }

                Integer currentPage = response.getResponse().getCurrentPage();
                Integer totalPages = response.getResponse().getPages();
                if (currentPage != null && totalPages != null && currentPage >= totalPages) {
                    break;
                }
                if (uniqueArticles.size() >= guardianProperties.getTopLimit()) {
                    break;
                }

                sleepQuietly(guardianProperties.getRequestIntervalMs());
            }

            if (uniqueArticles.size() >= guardianProperties.getTopLimit()) {
                break;
            }
        }

        fetchResult.setArticles(uniqueArticles.values().stream()
                .sorted((left, right) -> {
                    LocalDateTime rightTime = right.getPublishedAt();
                    LocalDateTime leftTime = left.getPublishedAt();
                    if (rightTime == null && leftTime == null) {
                        return 0;
                    }
                    if (rightTime == null) {
                        return -1;
                    }
                    if (leftTime == null) {
                        return 1;
                    }
                    return rightTime.compareTo(leftTime);
                })
                .limit(guardianProperties.getTopLimit())
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
        config.put("pageSize", guardianProperties.getPageSize());
        config.put("topLimit", guardianProperties.getTopLimit());
        config.put("maxPagesPerQuery", guardianProperties.getMaxPagesPerQuery());
        config.put("requestIntervalMs", guardianProperties.getRequestIntervalMs());
        config.put("sections", guardianProperties.getSections());
        config.put("queries", guardianProperties.getQueries());
        return config;
    }

    /**
     * 将 Guardian 结果转换为统一来源模型。
     *
     * @param item Guardian 单条结果
     * @param fetchDate 抓取日期
     * @return 统一来源模型
     */
    private NewsProviderArticle toProviderArticle(GuardianSearchResponse.ResultItem item, LocalDate fetchDate) {
        NewsProviderArticle article = new NewsProviderArticle();
        article.setProviderCode(PROVIDER_CODE);
        article.setSourceContentId(limit(item.getId(), 255));
        article.setSectionId(limit(item.getSectionId(), 100));
        article.setSectionName(limit(item.getSectionName(), 100));
        article.setPillarId(limit(item.getPillarId(), 100));
        article.setPillarName(limit(item.getPillarName(), 100));
        article.setTitle(limit(item.getWebTitle(), 500));
        article.setSummary(limit(item.getFields() == null ? null : item.getFields().getTrailText(), 5000));
        article.setContent(item.getFields() == null ? null : item.getFields().getBodyText());
        article.setAuthor(limit(item.getFields() == null ? null : item.getFields().getByline(), 255));
        article.setWebUrl(limit(item.getWebUrl(), 500));
        article.setApiUrl(limit(item.getApiUrl(), 500));
        article.setThumbnailUrl(limit(item.getFields() == null ? null : item.getFields().getThumbnail(), 500));
        article.setPublishedAt(parsePublishedAt(item.getWebPublicationDate(), fetchDate));
        article.setLang("en");
        article.setRawJson(toRawJson(item));
        return article;
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
     * @param item Guardian 单条结果
     * @return JSON 字符串
     */
    private String toRawJson(GuardianSearchResponse.ResultItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException exception) {
            return "{}";
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
