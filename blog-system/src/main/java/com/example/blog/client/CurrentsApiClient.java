package com.example.blog.client;

import com.example.blog.client.dto.CurrentsLatestNewsResponse;
import com.example.blog.config.CurrentsProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Currents API 客户端。
 */
@Component
public class CurrentsApiClient {

    /** Currents 要求日期参数使用 RFC3339 格式。 */
    private static final DateTimeFormatter RFC3339_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Resource(name = "currentsRestTemplate")
    private RestTemplate currentsRestTemplate;

    @Resource
    private CurrentsProperties currentsProperties;

    /**
     * 按日期、关键词和页码查询新闻。
     *
     * @param fetchDate 抓取日期
     * @param keyword 查询关键词
     * @param page 页码
     * @return Currents 查询结果
     */
    public CurrentsLatestNewsResponse searchNews(LocalDate fetchDate, String keyword, int page) {
        String category = resolveCategory();
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(currentsProperties.getBaseUrl())
                .path("/search")
                .queryParam("apiKey", currentsProperties.getApiKey())
                .queryParam("keywords", keyword)
                .queryParam("page_number", page)
                .queryParam("page_size", currentsProperties.getPageSize())
                .queryParam("language", currentsProperties.getLanguage())
                .queryParam("start_date", formatRfc3339(fetchDate))
                .queryParam("end_date", formatRfc3339(fetchDate.plusDays(1)));

        if (StringUtils.hasText(category)) {
            builder.queryParam("category", category);
        }

        try {
            return currentsRestTemplate.getForObject(
                    builder.encode().build().toUri(),
                    CurrentsLatestNewsResponse.class
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("调用 Currents API 失败: " + exception.getMessage(), exception);
        }
    }

    /**
     * 返回 Currents API 是否配置完整。
     *
     * @return true 表示配置完整
     */
    public boolean hasApiKey() {
        return StringUtils.hasText(currentsProperties.getApiKey());
    }

    /**
     * 将抓取日期转换为 RFC3339 时间。
     *
     * @param date 抓取日期
     * @return RFC3339 时间字符串
     */
    private String formatRfc3339(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC).format(RFC3339_FORMATTER);
    }

    /**
     * 兼容本地分类别名，避免参数与 Currents 版本不匹配。
     *
     * @return Currents 实际使用的分类
     */
    private String resolveCategory() {
        String category = currentsProperties.getCategory();
        if (!StringUtils.hasText(category)) {
            return null;
        }
        if ("science_technology".equalsIgnoreCase(category)) {
            return "technology";
        }
        return category;
    }
}
