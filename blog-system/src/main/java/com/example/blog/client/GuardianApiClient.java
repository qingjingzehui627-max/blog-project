package com.example.blog.client;

import com.example.blog.client.dto.GuardianSearchResponse;
import com.example.blog.config.GuardianProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * Guardian Open Platform API 客户端。
 */
@Component
public class GuardianApiClient {

    @Resource(name = "guardianRestTemplate")
    private RestTemplate guardianRestTemplate;

    @Resource
    private GuardianProperties guardianProperties;

    /**
     * 按日期和关键词查询新闻。
     *
     * @param fetchDate 抓取日期
     * @param query 查询关键词
     * @param page 页码
     * @return Guardian 搜索结果
     */
    public GuardianSearchResponse searchNews(LocalDate fetchDate, String query, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(guardianProperties.getBaseUrl())
                .path("/search")
                .queryParam("api-key", guardianProperties.getApiKey())
                .queryParam("q", query)
                .queryParam("page", page)
                .queryParam("page-size", guardianProperties.getPageSize())
                .queryParam("order-by", "newest")
                .queryParam("from-date", fetchDate)
                .queryParam("to-date", fetchDate)
                .queryParam("show-fields", "trailText,byline,thumbnail,bodyText");

        List<String> sections = guardianProperties.getSections();
        if (sections != null && !sections.isEmpty()) {
            String sectionValue = String.join("|", sections);
            if (StringUtils.hasText(sectionValue)) {
                builder.queryParam("section", sectionValue);
            }
        }

        try {
            return guardianRestTemplate.getForObject(
                    builder.encode().build().toUri(),
                    GuardianSearchResponse.class
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("调用 Guardian API 失败: " + exception.getMessage(), exception);
        }
    }
}
