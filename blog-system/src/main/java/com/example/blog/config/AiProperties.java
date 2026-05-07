package com.example.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 绑定 application 配置文件中的 AI 相关参数。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private static final double DEFAULT_TEMPERATURE = 0.7D;
    private static final int DEFAULT_DAILY_LIMIT = 10;
    private static final int DEFAULT_MAX_HISTORY = 10;
    private static final int DEFAULT_TIMEOUT_MS = 30000;
    private static final int DEFAULT_RAG_MAX_SOURCES = 4;
    private static final int DEFAULT_RAG_CANDIDATE_LIMIT = 20;
    private static final int DEFAULT_RAG_CHUNK_SIZE = 320;
    private static final int DEFAULT_RAG_CHUNK_OVERLAP = 60;
    private static final int DEFAULT_RAG_SNIPPET_LENGTH = 160;

    private boolean enabled;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    private String systemPrompt;
    private Double temperature;
    private Integer dailyLimit;
    private Integer maxHistory;
    private Integer timeoutMs;
    private List<String> models;
    private Boolean ragEnabled;
    private Integer ragMaxSources;
    private Integer ragCandidateLimit;
    private Integer ragChunkSize;
    private Integer ragChunkOverlap;
    private Integer ragSnippetLength;

    /**
     * 获取生效后的温度参数。
     *
     * @return 温度参数
     */
    public double getTemperature() {
        return temperature == null ? DEFAULT_TEMPERATURE : temperature;
    }

    /**
     * 获取生效后的每日调用上限。
     *
     * @return 每日调用上限
     */
    public int getDailyLimit() {
        return dailyLimit == null || dailyLimit <= 0 ? DEFAULT_DAILY_LIMIT : dailyLimit;
    }

    /**
     * 获取生效后的历史消息保留条数。
     *
     * @return 历史消息条数
     */
    public int getMaxHistory() {
        return maxHistory == null || maxHistory < 0 ? DEFAULT_MAX_HISTORY : maxHistory;
    }

    /**
     * 获取生效后的超时时间。
     *
     * @return 超时时间，单位毫秒
     */
    public int getTimeoutMs() {
        return timeoutMs == null || timeoutMs <= 0 ? DEFAULT_TIMEOUT_MS : timeoutMs;
    }

    /**
     * 获取生效后的 RAG 开关。
     * 未显式配置时默认开启，方便站内知识检索直接可用。
     *
     * @return RAG 是否开启
     */
    public boolean isRagEnabled() {
        return ragEnabled == null || ragEnabled;
    }

    /**
     * 获取生效后的 RAG 来源数量上限。
     *
     * @return 来源数量上限
     */
    public int getRagMaxSources() {
        return ragMaxSources == null || ragMaxSources <= 0 ? DEFAULT_RAG_MAX_SOURCES : ragMaxSources;
    }

    /**
     * 获取生效后的 RAG 候选文档数量。
     *
     * @return 候选文档数量
     */
    public int getRagCandidateLimit() {
        return ragCandidateLimit == null || ragCandidateLimit <= 0 ? DEFAULT_RAG_CANDIDATE_LIMIT : ragCandidateLimit;
    }

    /**
     * 获取生效后的分块大小。
     *
     * @return 分块大小
     */
    public int getRagChunkSize() {
        return ragChunkSize == null || ragChunkSize <= 0 ? DEFAULT_RAG_CHUNK_SIZE : ragChunkSize;
    }

    /**
     * 获取生效后的分块重叠长度。
     *
     * @return 分块重叠长度
     */
    public int getRagChunkOverlap() {
        return ragChunkOverlap == null || ragChunkOverlap < 0 ? DEFAULT_RAG_CHUNK_OVERLAP : ragChunkOverlap;
    }

    /**
     * 获取生效后的摘要片段长度。
     *
     * @return 摘要片段长度
     */
    public int getRagSnippetLength() {
        return ragSnippetLength == null || ragSnippetLength <= 0 ? DEFAULT_RAG_SNIPPET_LENGTH : ragSnippetLength;
    }
}
