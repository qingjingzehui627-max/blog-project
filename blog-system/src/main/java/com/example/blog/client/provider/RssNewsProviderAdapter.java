package com.example.blog.client.provider;

import com.example.blog.client.RssFeedClient;
import com.example.blog.client.dto.RssFeedItem;
import com.example.blog.client.provider.dto.NewsProviderArticle;
import com.example.blog.client.provider.dto.NewsProviderFetchResult;
import com.example.blog.config.RssProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RSS 数据源适配器。
 */
@Component
public class RssNewsProviderAdapter implements NewsProviderAdapter {

    private static final String PROVIDER_CODE = "rss";

    @Resource
    private RssProperties rssProperties;

    @Resource
    private RssFeedClient rssFeedClient;

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
        return "RSS";
    }

    /**
     * 判断当前数据源是否启用。
     *
     * @return true 表示启用
     */
    @Override
    public boolean isEnabled() {
        return rssProperties.isEnabled();
    }

    /**
     * 校验 RSS 抓取配置。
     *
     * @return 返回 null 表示配置有效，否则返回错误信息
     */
    @Override
    public String validateConfig() {
        if (!rssProperties.isEnabled()) {
            return "RSS 新闻抓取未启用";
        }
        if (rssProperties.getFeeds() == null || rssProperties.getFeeds().isEmpty()) {
            return "未配置 RSS Feed 列表";
        }
        return null;
    }

    /**
     * 执行一次 RSS 新闻抓取。
     *
     * @param fetchDate 抓取日期
     * @return 数据源抓取结果
     */
    @Override
    public NewsProviderFetchResult fetchArticles(LocalDate fetchDate) {
        NewsProviderFetchResult fetchResult = new NewsProviderFetchResult();
        fetchResult.setProviderCode(PROVIDER_CODE);

        LinkedHashMap<String, NewsProviderArticle> uniqueArticles = new LinkedHashMap<>();
        List<String> feeds = rssProperties.getFeeds();
        if (feeds == null) {
            return fetchResult;
        }

        // RSS 没有统一分页能力，这里按 Feed 顺序抓取，
        // 并在转换阶段过滤掉非目标日期的内容。
        for (String feedUrl : feeds) {
            if (!StringUtils.hasText(feedUrl)) {
                continue;
            }

            List<RssFeedItem> items = rssFeedClient.fetchFeed(feedUrl);
            fetchResult.setRequestCount(fetchResult.getRequestCount() + 1);
            for (RssFeedItem item : items) {
                if (item == null) {
                    continue;
                }
                if (item.getPublishedAt() != null && !fetchDate.equals(item.getPublishedAt().toLocalDate())) {
                    continue;
                }
                NewsProviderArticle article = toProviderArticle(item, fetchDate);
                if (!StringUtils.hasText(article.getSourceContentId())) {
                    continue;
                }
                uniqueArticles.putIfAbsent(article.getSourceContentId(), article);
            }

            if (uniqueArticles.size() >= rssProperties.getTopLimit()) {
                break;
            }
            sleepQuietly(rssProperties.getRequestIntervalMs());
        }

        fetchResult.setArticles(uniqueArticles.values().stream()
                .sorted((left, right) -> comparePublishedTime(right.getPublishedAt(), left.getPublishedAt()))
                .limit(rssProperties.getTopLimit())
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
        config.put("topLimit", rssProperties.getTopLimit());
        config.put("requestIntervalMs", rssProperties.getRequestIntervalMs());
        config.put("defaultLanguage", rssProperties.getDefaultLanguage());
        config.put("feeds", rssProperties.getFeeds());
        return config;
    }

    /**
     * 将 RSS 结果转换为统一来源模型。
     *
     * @param item RSS 单条结果
     * @param fetchDate 抓取日期
     * @return 统一来源模型
     */
    private NewsProviderArticle toProviderArticle(RssFeedItem item, LocalDate fetchDate) {
        NewsProviderArticle article = new NewsProviderArticle();
        article.setProviderCode(PROVIDER_CODE);
        article.setSourceContentId(buildSourceContentId(item));
        article.setSectionName(item.getFeedUrl());
        article.setTitle(limit(item.getTitle(), 500));
        article.setSummary(limit(item.getSummary(), 5000));
        article.setAuthor(limit(item.getAuthor(), 255));
        article.setWebUrl(limit(item.getUrl(), 500));
        article.setPublishedAt(item.getPublishedAt() != null ? item.getPublishedAt() : fetchDate.atStartOfDay());
        article.setLang(rssProperties.getDefaultLanguage());
        article.setRawJson("{}");
        return article;
    }

    /**
     * 构造来源内容 ID。
     *
     * @param item RSS 单条结果
     * @return 来源内容 ID
     */
    private String buildSourceContentId(RssFeedItem item) {
        if (StringUtils.hasText(item.getUrl())) {
            return sha256(item.getUrl());
        }
        if (StringUtils.hasText(item.getTitle())) {
            return sha256(item.getTitle());
        }
        return "";
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
            throw new IllegalStateException("计算 RSS 来源哈希失败", exception);
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
