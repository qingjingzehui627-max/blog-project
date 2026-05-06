package com.example.blog.client.provider;

import com.example.blog.client.provider.dto.NewsProviderFetchResult;

import java.time.LocalDate;
import java.util.Map;

/**
 * 新闻数据源适配器。
 */
public interface NewsProviderAdapter {

    /**
     * 返回数据源编码。
     *
     * @return 数据源编码
     */
    String getProviderCode();

    /**
     * 返回数据源展示名称。
     *
     * @return 数据源展示名称
     */
    String getDisplayName();

    /**
     * 判断当前数据源是否启用。
     *
     * @return true 表示启用
     */
    boolean isEnabled();

    /**
     * 校验数据源抓取配置。
     *
     * @return 返回 null 表示配置有效，否则返回错误信息
     */
    String validateConfig();

    /**
     * 执行一次抓取。
     *
     * @param fetchDate 抓取日期
     * @return 数据源抓取结果
     */
    NewsProviderFetchResult fetchArticles(LocalDate fetchDate);

    /**
     * 返回后台展示所需的配置摘要。
     *
     * @return 配置摘要
     */
    Map<String, Object> getAdminConfig();
}
