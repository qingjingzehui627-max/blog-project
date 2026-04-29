package com.example.blog.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Guardian 搜索接口响应对象。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianSearchResponse {
    private ResponseData response;

    /**
     * 外层响应数据。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseData {
        private String status;
        private Integer total;
        private Integer currentPage;
        private Integer pages;
        private List<ResultItem> results;
    }

    /**
     * 单条新闻结果。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultItem {
        private String id;
        private String type;
        private String sectionId;
        private String sectionName;
        private String webPublicationDate;
        private String webTitle;
        private String webUrl;
        private String apiUrl;
        private String pillarId;
        private String pillarName;
        private Fields fields;
    }

    /**
     * 新闻扩展字段。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fields {
        private String trailText;
        private String byline;
        private String bodyText;
        private String thumbnail;
    }
}
