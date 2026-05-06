package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.config.AiProperties;
import com.example.blog.config.GuardianProperties;
import com.example.blog.config.NewsSummaryProperties;
import com.example.blog.dto.NewsAiSummaryResponse;
import com.example.blog.entity.NewsAiSummary;
import com.example.blog.entity.NewsArticle;
import com.example.blog.mapper.NewsAiSummaryMapper;
import com.example.blog.mapper.NewsArticleMapper;
import com.example.blog.service.NewsAiSummaryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * 新闻 AI 摘要服务实现。
 */
@Service
public class NewsAiSummaryServiceImpl implements NewsAiSummaryService {

    private static final String SUMMARY_SYSTEM_PROMPT = "你是科技新闻摘要助手。请基于提供的新闻内容生成简洁、客观、不编造的中文摘要，只输出摘要正文，不要标题，不要序号。";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private static final Map<String, Integer> LOCAL_QUOTA_CACHE = new ConcurrentHashMap<>();

    @Resource
    private AiProperties aiProperties;

    @Resource
    private NewsSummaryProperties newsSummaryProperties;

    @Resource
    private GuardianProperties guardianProperties;

    @Resource(name = "aiRestTemplate")
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private NewsArticleMapper newsArticleMapper;

    @Resource
    private NewsAiSummaryMapper newsAiSummaryMapper;

    /**
     * 查询指定新闻的 AI 摘要。
     *
     * @param articleId 新闻主键
     * @return AI 摘要
     */
    @Override
    public NewsAiSummaryResponse getSummaryByArticleId(Long articleId) {
        if (articleId == null) {
            return null;
        }
        NewsAiSummary summary = newsAiSummaryMapper.selectOne(
                new LambdaQueryWrapper<NewsAiSummary>()
                        .eq(NewsAiSummary::getArticleId, articleId)
                        .last("LIMIT 1")
        );
        return toResponse(summary);
    }

    /**
     * 手动生成单条新闻摘要。
     *
     * @param articleId 新闻主键
     * @param triggerType 触发方式
     * @param force 是否强制重生成
     * @return 生成结果
     */
    @Override
    public Map<String, Object> generateSummary(Long articleId, String triggerType, boolean force) {
        NewsArticle article = loadArticle(articleId);
        SummaryProcessResult processResult = generateForArticle(article, triggerType, force);

        Map<String, Object> result = new HashMap<>();
        result.put("articleId", articleId);
        result.put("title", article.getTitle());
        result.put("status", processResult.getStatus());
        result.put("message", processResult.getMessage());
        result.put("quota", getDailyQuota(LocalDate.now()));
        result.put("summary", toResponse(processResult.getSummary()));
        return result;
    }

    /**
     * 生成指定日期前十条热度新闻摘要。
     *
     * @param fetchDate 新闻抓取日期
     * @param triggerType 触发方式
     * @param force 是否强制重生成
     * @return 批量生成结果
     */
    @Override
    public Map<String, Object> generateTopSummaries(LocalDate fetchDate, String triggerType, boolean force) {
        ensureSummaryEnabled();
        LocalDate targetDate = resolveTargetDate(fetchDate);
        List<NewsArticle> topArticles = newsArticleMapper.selectList(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getStatus, 1)
                        .eq(NewsArticle::getFetchDate, targetDate)
                        .isNotNull(NewsArticle::getRankOrder)
                        .orderByAsc(NewsArticle::getRankOrder)
                        .last("LIMIT " + Math.max(newsSummaryProperties.getAutoTopLimit(), 1))
        );

        int generatedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        List<Map<String, Object>> items = new ArrayList<>();

        for (NewsArticle article : topArticles) {
            SummaryProcessResult processResult = generateForArticle(article, triggerType, force);
            if (STATUS_SUCCESS.equals(processResult.getStatus())) {
                generatedCount++;
            } else if (STATUS_FAILED.equals(processResult.getStatus())) {
                failedCount++;
            } else {
                skippedCount++;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("articleId", article.getId());
            item.put("title", article.getTitle());
            item.put("rankOrder", article.getRankOrder());
            item.put("status", processResult.getStatus());
            item.put("message", processResult.getMessage());
            items.add(item);

            // 达到每日额度后直接停止，避免后续继续发起模型请求。
            if (processResult.isQuotaLimited()) {
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("fetchDate", targetDate);
        result.put("limit", Math.max(newsSummaryProperties.getAutoTopLimit(), 1));
        result.put("generatedCount", generatedCount);
        result.put("skippedCount", skippedCount);
        result.put("failedCount", failedCount);
        result.put("items", items);
        result.put("quota", getDailyQuota(LocalDate.now()));
        return result;
    }

    /**
     * 查询每日摘要额度。
     *
     * @param date 统计日期
     * @return 额度信息
     */
    @Override
    public Map<String, Object> getDailyQuota(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        int used = getUsedCount(targetDate);
        int dailyLimit = Math.max(newsSummaryProperties.getDailyLimit(), 0);

        Map<String, Object> result = new HashMap<>();
        result.put("date", targetDate);
        result.put("limit", dailyLimit);
        result.put("used", used);
        result.put("remaining", Math.max(0, dailyLimit - used));
        return result;
    }

    /**
     * 执行每日自动摘要任务。
     */
    @Override
    public void generateDailyTopSummaries() {
        if (!newsSummaryProperties.isEnabled() || !newsSummaryProperties.isAutoEnabled()) {
            return;
        }
        LocalDate targetDate = LocalDate.now().plusDays(guardianProperties.getFetchOffsetDays());
        generateTopSummaries(targetDate, "AUTO", false);
    }

    /**
     * 生成单条新闻摘要。
     *
     * @param article 新闻实体
     * @param triggerType 触发方式
     * @param force 是否强制重生成
     * @return 处理结果
     */
    private SummaryProcessResult generateForArticle(NewsArticle article, String triggerType, boolean force) {
        ensureSummaryEnabled();
        SummaryProcessResult result = new SummaryProcessResult();
        result.setStatus(STATUS_SKIPPED);

        NewsAiSummary existingSummary = getSummaryEntity(article.getId());
        if (!force && existingSummary != null && STATUS_SUCCESS.equals(existingSummary.getSummaryStatus())
                && StringUtils.hasText(existingSummary.getSummaryText())) {
            result.setMessage("该新闻已有可用摘要，已跳过生成");
            result.setSummary(existingSummary);
            return result;
        }

        LocalDate quotaDate = LocalDate.now();
        if (isQuotaExceeded(quotaDate)) {
            result.setMessage("今日摘要额度已用尽");
            result.setQuotaLimited(true);
            return result;
        }

        int maxAttempts = Math.max(newsSummaryProperties.getMaxRetries(), 0) + 1;
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String summaryText = callSummaryProvider(article);
                if (!StringUtils.hasText(summaryText)) {
                    throw new IllegalStateException("模型返回的摘要为空");
                }

                NewsAiSummary savedSummary = saveSuccessSummary(
                        article,
                        existingSummary,
                        summaryText.trim(),
                        triggerType,
                        attempt - 1
                );
                incrementUsage(quotaDate);

                result.setStatus(STATUS_SUCCESS);
                result.setMessage("摘要生成成功");
                result.setSummary(savedSummary);
                return result;
            } catch (Exception exception) {
                lastException = exception;
            }
        }

        NewsAiSummary failedSummary = saveFailedSummary(
                article,
                existingSummary,
                triggerType,
                maxAttempts - 1,
                lastException == null ? "摘要生成失败" : lastException.getMessage()
        );
        result.setStatus(STATUS_FAILED);
        result.setMessage(lastException == null ? "摘要生成失败" : lastException.getMessage());
        result.setSummary(failedSummary);
        return result;
    }

    /**
     * 查询新闻主表中的有效新闻。
     *
     * @param articleId 新闻主键
     * @return 新闻实体
     */
    private NewsArticle loadArticle(Long articleId) {
        NewsArticle article = newsArticleMapper.selectOne(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getId, articleId)
                        .eq(NewsArticle::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (article == null) {
            throw new ResponseStatusException(NOT_FOUND, "新闻不存在或已下线");
        }
        return article;
    }

    /**
     * 查询指定新闻的摘要实体。
     *
     * @param articleId 新闻主键
     * @return 摘要实体
     */
    private NewsAiSummary getSummaryEntity(Long articleId) {
        return newsAiSummaryMapper.selectOne(
                new LambdaQueryWrapper<NewsAiSummary>()
                        .eq(NewsAiSummary::getArticleId, articleId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 调用 AI 模型生成摘要。
     *
     * @param article 新闻实体
     * @return 生成后的摘要文本
     */
    private String callSummaryProvider(NewsArticle article) {
        if (!aiProperties.isEnabled()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI 功能未开启，无法生成摘要");
        }
        String provider = aiProperties.getProvider() == null ? "openai" : aiProperties.getProvider().trim().toLowerCase();
        if ("ollama".equals(provider)) {
            return callOllama(article);
        }
        return callOpenAiCompatible(article);
    }

    /**
     * 调用兼容 OpenAI 的接口生成摘要。
     *
     * @param article 新闻实体
     * @return 摘要文本
     */
    private String callOpenAiCompatible(NewsArticle article) {
        List<Map<String, String>> messages = buildSummaryMessages(article);

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("instructions", SUMMARY_SYSTEM_PROMPT);
        body.put("messages", messages);

        HttpHeaders headers = createJsonHeaders();
        if (StringUtils.hasText(aiProperties.getApiKey())) {
            headers.setBearerAuth(aiProperties.getApiKey().trim());
        }

        String url = trimTrailingSlash(aiProperties.getBaseUrl()) + "/chat/completions";
        Map<String, Object> response = readJsonResponse(url, body, headers);
        List<Map<String, Object>> choices = castList(response.get("choices"));
        if (choices.isEmpty()) {
            throw new IllegalStateException("AI 返回摘要内容为空");
        }
        Map<String, Object> choice = castMap(choices.get(0));
        Map<String, Object> replyMessage = castMap(choice.get("message"));
        Object content = replyMessage.get("content");
        return extractContent(content);
    }

    /**
     * 调用 Ollama 接口生成摘要。
     *
     * @param article 新闻实体
     * @return 摘要文本
     */
    private String callOllama(NewsArticle article) {
        List<Map<String, String>> messages = buildSummaryMessages(article);

        Map<String, Object> body = new HashMap<>();
        body.put("model", aiProperties.getModel());
        body.put("messages", messages);
        body.put("stream", false);

        String url = trimTrailingSlash(aiProperties.getBaseUrl()) + "/api/chat";
        Map<String, Object> response = readJsonResponse(url, body, createJsonHeaders());
        Map<String, Object> replyMessage = castMap(response.get("message"));
        return extractContent(replyMessage.get("content"));
    }

    /**
     * 构造摘要提示词。
     *
     * @param article 新闻实体
     * @return 消息列表
     */
    private List<Map<String, String>> buildSummaryMessages(NewsArticle article) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(createMessage("system", SUMMARY_SYSTEM_PROMPT));
        messages.add(createMessage("user", buildSummaryPrompt(article)));
        return messages;
    }

    /**
     * 拼接摘要输入内容。
     *
     * @param article 新闻实体
     * @return 摘要提示词
     */
    private String buildSummaryPrompt(NewsArticle article) {
        StringBuilder builder = new StringBuilder();
        builder.append("请为下面这篇科技新闻生成 80 到 150 字的中文摘要。\n");
        builder.append("要求：突出关键信息、保持客观、不要编造原文没有的结论。\n\n");
        builder.append("标题：").append(defaultText(article.getTitle())).append("\n");
        builder.append("分类：").append(defaultText(article.getCategoryName())).append("\n");
        builder.append("发布时间：").append(article.getPublishedAt() == null ? "-" : article.getPublishedAt()).append("\n");
        builder.append("原始摘要：").append(defaultText(article.getSummary())).append("\n");
        builder.append("正文：").append(limitText(article.getContent(), newsSummaryProperties.getContentMaxLength())).append("\n");
        return builder.toString();
    }

    /**
     * 保存成功摘要。
     *
     * @param article 新闻实体
     * @param existingSummary 旧摘要
     * @param summaryText 新摘要文本
     * @param triggerType 触发方式
     * @param retryCount 重试次数
     * @return 保存后的摘要实体
     */
    private NewsAiSummary saveSuccessSummary(
            NewsArticle article,
            NewsAiSummary existingSummary,
            String summaryText,
            String triggerType,
            int retryCount) {
        LocalDateTime now = LocalDateTime.now();
        NewsAiSummary summary = existingSummary != null ? existingSummary : new NewsAiSummary();
        summary.setArticleId(article.getId());
        summary.setFetchDate(article.getFetchDate());
        summary.setSummaryText(summaryText);
        summary.setSummaryStatus(STATUS_SUCCESS);
        summary.setTriggerType(defaultTriggerType(triggerType));
        summary.setModelName(aiProperties.getModel());
        summary.setPromptVersion(newsSummaryProperties.getPromptVersion());
        summary.setGeneratedAt(now);
        summary.setRetryCount(retryCount);
        summary.setErrorMessage(null);
        if (summary.getId() == null) {
            summary.setCreatedAt(now);
            summary.setUpdatedAt(now);
            newsAiSummaryMapper.insert(summary);
        } else {
            summary.setUpdatedAt(now);
            newsAiSummaryMapper.updateById(summary);
        }
        return summary;
    }

    /**
     * 保存失败摘要记录。
     *
     * @param article 新闻实体
     * @param existingSummary 旧摘要
     * @param triggerType 触发方式
     * @param retryCount 重试次数
     * @param errorMessage 错误信息
     * @return 保存后的摘要实体
     */
    private NewsAiSummary saveFailedSummary(
            NewsArticle article,
            NewsAiSummary existingSummary,
            String triggerType,
            int retryCount,
            String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        NewsAiSummary summary = existingSummary != null ? existingSummary : new NewsAiSummary();
        summary.setArticleId(article.getId());
        summary.setFetchDate(article.getFetchDate());
        summary.setSummaryStatus(STATUS_FAILED);
        summary.setTriggerType(defaultTriggerType(triggerType));
        summary.setModelName(aiProperties.getModel());
        summary.setPromptVersion(newsSummaryProperties.getPromptVersion());
        summary.setRetryCount(retryCount);
        summary.setErrorMessage(limitText(errorMessage, 1000));
        if (summary.getId() == null) {
            summary.setCreatedAt(now);
            summary.setUpdatedAt(now);
            newsAiSummaryMapper.insert(summary);
        } else {
            summary.setUpdatedAt(now);
            newsAiSummaryMapper.updateById(summary);
        }
        return summary;
    }

    /**
     * 校验摘要功能是否开启。
     */
    private void ensureSummaryEnabled() {
        if (!newsSummaryProperties.isEnabled()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "新闻 AI 摘要功能未开启");
        }
    }

    /**
     * 解析目标新闻日期。
     *
     * @param fetchDate 传入日期
     * @return 实际使用的日期
     */
    private LocalDate resolveTargetDate(LocalDate fetchDate) {
        if (fetchDate != null) {
            return fetchDate;
        }
        NewsArticle latestArticle = newsArticleMapper.selectOne(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getStatus, 1)
                        .orderByDesc(NewsArticle::getFetchDate)
                        .orderByAsc(NewsArticle::getRankOrder)
                        .last("LIMIT 1")
        );
        return latestArticle == null ? LocalDate.now().plusDays(guardianProperties.getFetchOffsetDays()) : latestArticle.getFetchDate();
    }

    /**
     * 判断当天额度是否已用尽。
     *
     * @param quotaDate 配额日期
     * @return true 表示已用尽
     */
    private boolean isQuotaExceeded(LocalDate quotaDate) {
        return getUsedCount(quotaDate) >= Math.max(newsSummaryProperties.getDailyLimit(), 0);
    }

    /**
     * 读取当天已使用摘要次数。
     *
     * @param quotaDate 配额日期
     * @return 已使用次数
     */
    private int getUsedCount(LocalDate quotaDate) {
        try {
            String value = stringRedisTemplate.opsForValue().get(buildQuotaKey(quotaDate));
            if (!StringUtils.hasText(value)) {
                return LOCAL_QUOTA_CACHE.getOrDefault(buildQuotaKey(quotaDate), 0);
            }
            return Integer.parseInt(value);
        } catch (Exception exception) {
            return LOCAL_QUOTA_CACHE.getOrDefault(buildQuotaKey(quotaDate), 0);
        }
    }

    /**
     * 增加当天摘要使用次数。
     *
     * @param quotaDate 配额日期
     */
    private void incrementUsage(LocalDate quotaDate) {
        String key = buildQuotaKey(quotaDate);
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, ttlToTomorrow());
            }
        } catch (Exception exception) {
            LOCAL_QUOTA_CACHE.merge(key, 1, Integer::sum);
        }
    }

    /**
     * 计算到次日零点的过期时间。
     *
     * @return 过期时长
     */
    private Duration ttlToTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, tomorrow);
    }

    /**
     * 生成摘要配额缓存键。
     *
     * @param quotaDate 配额日期
     * @return Redis 键
     */
    private String buildQuotaKey(LocalDate quotaDate) {
        return "ai:news-summary:quota:" + quotaDate;
    }

    /**
     * 读取 JSON 响应。
     *
     * @param url 请求地址
     * @param body 请求体
     * @param headers 请求头
     * @return 解析后的 JSON
     */
    private Map<String, Object> readJsonResponse(String url, Map<String, Object> body, HttpHeaders headers) {
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        String responseBody = responseEntity.getBody();
        MediaType contentType = responseEntity.getHeaders().getContentType();

        if (!StringUtils.hasText(responseBody)) {
            throw new IllegalStateException("AI 接口返回为空");
        }

        String trimmed = responseBody.trim();
        boolean looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[");
        boolean isJsonContentType = contentType != null && (
                MediaType.APPLICATION_JSON.includes(contentType) ||
                        contentType.getSubtype().toLowerCase().contains("json")
        );
        if (!looksLikeJson && !isJsonContentType) {
            throw new IllegalStateException("AI 接口未返回有效 JSON");
        }

        try {
            return objectMapper.readValue(trimmed, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("AI 接口返回的不是有效 JSON");
        }
    }

    /**
     * 提取模型返回的文本内容。
     *
     * @param content 返回内容
     * @return 文本
     */
    private String extractContent(Object content) {
        if (content instanceof String) {
            return ((String) content).trim();
        }
        if (content instanceof List) {
            StringBuilder builder = new StringBuilder();
            for (Object item : (List<?>) content) {
                Map<String, Object> part = castMap(item);
                Object text = part.get("text");
                if (text != null) {
                    builder.append(text);
                }
            }
            return builder.toString().trim();
        }
        throw new IllegalStateException("AI 返回内容格式不支持");
    }

    /**
     * 创建消息对象。
     *
     * @param role 角色
     * @param content 内容
     * @return 消息对象
     */
    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 创建 JSON 请求头。
     *
     * @return 请求头
     */
    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * 移除地址末尾的斜杠。
     *
     * @param baseUrl 基础地址
     * @return 处理后的地址
     */
    private String trimTrailingSlash(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    /**
     * 截断过长文本。
     *
     * @param value 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(maxLength, 0));
    }

    /**
     * 返回默认文本，避免空值影响提示词。
     *
     * @param value 原始文本
     * @return 处理后的文本
     */
    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    /**
     * 统一触发方式文本。
     *
     * @param triggerType 触发方式
     * @return 触发方式
     */
    private String defaultTriggerType(String triggerType) {
        return StringUtils.hasText(triggerType) ? triggerType.trim() : "MANUAL";
    }

    /**
     * 转换摘要响应。
     *
     * @param summary 摘要实体
     * @return 摘要响应
     */
    private NewsAiSummaryResponse toResponse(NewsAiSummary summary) {
        if (summary == null) {
            return null;
        }
        NewsAiSummaryResponse response = new NewsAiSummaryResponse();
        response.setId(summary.getId());
        response.setArticleId(summary.getArticleId());
        response.setFetchDate(summary.getFetchDate());
        response.setSummaryText(summary.getSummaryText());
        response.setSummaryStatus(summary.getSummaryStatus());
        response.setTriggerType(summary.getTriggerType());
        response.setModelName(summary.getModelName());
        response.setPromptVersion(summary.getPromptVersion());
        response.setGeneratedAt(summary.getGeneratedAt());
        response.setRetryCount(summary.getRetryCount());
        response.setErrorMessage(summary.getErrorMessage());
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    /**
     * 单条摘要处理结果。
     */
    private static class SummaryProcessResult {
        private String status;
        private String message;
        private boolean quotaLimited;
        private NewsAiSummary summary;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isQuotaLimited() {
            return quotaLimited;
        }

        public void setQuotaLimited(boolean quotaLimited) {
            this.quotaLimited = quotaLimited;
        }

        public NewsAiSummary getSummary() {
            return summary;
        }

        public void setSummary(NewsAiSummary summary) {
            this.summary = summary;
        }
    }
}
