package com.example.blog.client;

import com.example.blog.client.dto.NewsApiEverythingResponse;
import com.example.blog.config.NewsApiProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * NewsAPI 客户端。
 */
@Component
public class NewsApiClient {

    @Resource(name = "newsApiRestTemplate")
    private RestTemplate newsApiRestTemplate;

    @Resource
    private NewsApiProperties newsApiProperties;

    /**
     * 按日期、关键词和页码查询新闻。
     *
     * @param fetchDate 抓取日期
     * @param query 查询关键词
     * @param page 页码
     * @return NewsAPI 查询结果
     */
    public NewsApiEverythingResponse searchNews(LocalDate fetchDate, String query, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(newsApiProperties.getBaseUrl())
                .path("/everything")
                .queryParam("apiKey", newsApiProperties.getApiKey())
                .queryParam("q", query)
                .queryParam("page", page)
                .queryParam("pageSize", newsApiProperties.getPageSize())
                .queryParam("sortBy", newsApiProperties.getSortBy())
                .queryParam("from", fetchDate)
                .queryParam("to", fetchDate.plusDays(1))
                .queryParam("language", newsApiProperties.getLanguage());

        List<String> domains = newsApiProperties.getDomains();
        if (domains != null && !domains.isEmpty()) {
            String domainValue = String.join(",", domains);
            if (StringUtils.hasText(domainValue)) {
                builder.queryParam("domains", domainValue);
            }
        }

        try {
            return newsApiRestTemplate.getForObject(
                    builder.encode().build().toUri(),
                    NewsApiEverythingResponse.class
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("调用 NewsAPI 失败: " + exception.getMessage(), exception);
        }
    }
}
