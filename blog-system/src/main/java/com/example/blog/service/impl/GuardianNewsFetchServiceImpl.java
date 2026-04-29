package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.classifier.NewsClassifier;
import com.example.blog.client.GuardianApiClient;
import com.example.blog.client.dto.GuardianSearchResponse;
import com.example.blog.config.GuardianProperties;
import com.example.blog.entity.NewsArticle;
import com.example.blog.entity.NewsFetchJobLog;
import com.example.blog.mapper.NewsArticleMapper;
import com.example.blog.mapper.NewsFetchJobLogMapper;
import com.example.blog.service.GuardianNewsFetchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Guardian 新闻抓取服务实现。
 */
@Service
public class GuardianNewsFetchServiceImpl implements GuardianNewsFetchService {

    private static final String SOURCE = "guardian";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String TRIGGER_SCHEDULED = "SCHEDULED";

    @Resource
    private GuardianProperties guardianProperties;

    @Resource
    private GuardianApiClient guardianApiClient;

    @Resource
    private NewsArticleMapper newsArticleMapper;

    @Resource
    private NewsFetchJobLogMapper newsFetchJobLogMapper;

    @Resource
    private NewsClassifier newsClassifier;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 执行一次新闻抓取任务。
     *
     * @param fetchDate 抓取日期
     * @param triggerType 触发方式
     * @return 抓取结果
     */
    @Override
    public Map<String, Object> fetchNews(LocalDate fetchDate, String triggerType) {
        LocalDate targetDate = fetchDate != null ? fetchDate : LocalDate.now();
        String safeTriggerType = StringUtils.hasText(triggerType) ? triggerType : "MANUAL";
        NewsFetchJobLog jobLog = createJobLog(targetDate, safeTriggerType);

        if (!guardianProperties.isEnabled()) {
            return skipJob(jobLog, "Guardian 新闻抓取未启用");
        }
        if (!StringUtils.hasText(guardianProperties.getApiKey())) {
            return skipJob(jobLog, "未配置 Guardian API Key");
        }
        if (TRIGGER_SCHEDULED.equalsIgnoreCase(safeTriggerType) && hasSuccessfulScheduledJob(targetDate)) {
            return skipJob(jobLog, "当天的定时抓取已成功执行，跳过重复任务");
        }

        int requestCount = 0;
        int insertedCount = 0;
        int updatedCount = 0;
        int failedCount = 0;
        LinkedHashMap<String, GuardianSearchResponse.ResultItem> uniqueResults = new LinkedHashMap<>();

        try {
            List<String> queries = guardianProperties.getQueries() == null
                    ? new ArrayList<>()
                    : guardianProperties.getQueries();

            // 多个查询词抓回来的结果可能会重复，这里先按 Guardian 内容 ID 去重，
            // 再统一排序取 Top N，避免同一篇新闻因为命中多个关键词而重复入库。
            for (String query : queries) {
                if (!StringUtils.hasText(query)) {
                    continue;
                }
                for (int page = 1; page <= guardianProperties.getMaxPagesPerQuery(); page++) {
                    GuardianSearchResponse response = guardianApiClient.searchNews(targetDate, query, page);
                    requestCount++;

                    List<GuardianSearchResponse.ResultItem> results = response != null && response.getResponse() != null
                            ? response.getResponse().getResults()
                            : null;
                    if (results == null || results.isEmpty()) {
                        break;
                    }

                    for (GuardianSearchResponse.ResultItem item : results) {
                        if (item != null && StringUtils.hasText(item.getId())) {
                            uniqueResults.putIfAbsent(item.getId(), item);
                        }
                    }

                    Integer currentPage = response.getResponse().getCurrentPage();
                    Integer pages = response.getResponse().getPages();
                    if (currentPage != null && pages != null && currentPage >= pages) {
                        break;
                    }
                    if (uniqueResults.size() >= guardianProperties.getTopLimit()) {
                        break;
                    }

                    // Guardian 开发者 Key 有调用频率限制，这里主动做节流，避免触发限流导致整批任务失败。
                    sleepQuietly(guardianProperties.getRequestIntervalMs());
                }

                if (uniqueResults.size() >= guardianProperties.getTopLimit()) {
                    break;
                }
            }

            List<NewsArticle> articles = uniqueResults.values().stream()
                    .map(item -> toNewsArticle(item, targetDate))
                    .filter(article -> StringUtils.hasText(article.getTitle()) && StringUtils.hasText(article.getWebUrl()))
                    .sorted(Comparator.comparing(NewsArticle::getPublishedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(guardianProperties.getTopLimit())
                    .toList();

            for (int index = 0; index < articles.size(); index++) {
                NewsArticle article = articles.get(index);
                article.setRankOrder(index + 1);
                boolean inserted = saveOrUpdateArticle(article);
                if (inserted) {
                    insertedCount++;
                } else {
                    updatedCount++;
                }
            }

            jobLog.setStatus(STATUS_SUCCESS);
            jobLog.setRequestCount(requestCount);
            jobLog.setFetchedCount(articles.size());
            jobLog.setInsertedCount(insertedCount);
            jobLog.setUpdatedCount(updatedCount);
            jobLog.setFailedCount(failedCount);
            jobLog.setFinishedAt(LocalDateTime.now());
            newsFetchJobLogMapper.updateById(jobLog);

            Map<String, Object> result = new HashMap<>();
            result.put("status", STATUS_SUCCESS);
            result.put("jobId", jobLog.getId());
            result.put("fetchDate", targetDate);
            result.put("requestCount", requestCount);
            result.put("fetchedCount", articles.size());
            result.put("insertedCount", insertedCount);
            result.put("updatedCount", updatedCount);
            return result;
        } catch (Exception exception) {
            failedCount++;
            jobLog.setStatus(STATUS_FAILED);
            jobLog.setRequestCount(requestCount);
            jobLog.setInsertedCount(insertedCount);
            jobLog.setUpdatedCount(updatedCount);
            jobLog.setFailedCount(failedCount);
            jobLog.setFinishedAt(LocalDateTime.now());
            jobLog.setErrorMessage(limit(exception.getMessage(), 2000));
            newsFetchJobLogMapper.updateById(jobLog);

            Map<String, Object> result = new HashMap<>();
            result.put("status", STATUS_FAILED);
            result.put("jobId", jobLog.getId());
            result.put("fetchDate", targetDate);
            result.put("message", exception.getMessage());
            return result;
        }
    }

    /**
     * 分页查询抓取日志。
     *
     * @param page 页码
     * @param size 每页数量
     * @return 抓取日志结果
     */
    @Override
    public Map<String, Object> getJobLogs(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        LambdaQueryWrapper<NewsFetchJobLog> countWrapper = new LambdaQueryWrapper<>();
        Long total = newsFetchJobLogMapper.selectCount(countWrapper);

        List<NewsFetchJobLog> list = newsFetchJobLogMapper.selectList(
                new LambdaQueryWrapper<NewsFetchJobLog>()
                        .orderByDesc(NewsFetchJobLog::getStartedAt)
                        .orderByDesc(NewsFetchJobLog::getId)
                        .last("LIMIT " + (safePage - 1) * safeSize + ", " + safeSize)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total);
        return result;
    }

    /**
     * 创建一条抓取任务日志。
     *
     * @param targetDate 抓取日期
     * @param triggerType 触发方式
     * @return 任务日志
     */
    private NewsFetchJobLog createJobLog(LocalDate targetDate, String triggerType) {
        NewsFetchJobLog jobLog = new NewsFetchJobLog();
        jobLog.setSource(SOURCE);
        jobLog.setJobDate(targetDate);
        jobLog.setTriggerType(triggerType);
        jobLog.setStatus(STATUS_RUNNING);
        jobLog.setRequestCount(0);
        jobLog.setFetchedCount(0);
        jobLog.setInsertedCount(0);
        jobLog.setUpdatedCount(0);
        jobLog.setFailedCount(0);
        jobLog.setStartedAt(LocalDateTime.now());
        newsFetchJobLogMapper.insert(jobLog);
        return jobLog;
    }

    /**
     * 将任务标记为跳过。
     *
     * @param jobLog 任务日志
     * @param message 跳过原因
     * @return 返回结果
     */
    private Map<String, Object> skipJob(NewsFetchJobLog jobLog, String message) {
        jobLog.setStatus(STATUS_SKIPPED);
        jobLog.setFinishedAt(LocalDateTime.now());
        jobLog.setErrorMessage(limit(message, 2000));
        newsFetchJobLogMapper.updateById(jobLog);

        Map<String, Object> result = new HashMap<>();
        result.put("status", STATUS_SKIPPED);
        result.put("jobId", jobLog.getId());
        result.put("fetchDate", jobLog.getJobDate());
        result.put("message", message);
        return result;
    }

    /**
     * 判断当天是否已有成功的定时任务。
     *
     * @param targetDate 抓取日期
     * @return 是否已成功执行
     */
    private boolean hasSuccessfulScheduledJob(LocalDate targetDate) {
        Long count = newsFetchJobLogMapper.selectCount(
                new LambdaQueryWrapper<NewsFetchJobLog>()
                        .eq(NewsFetchJobLog::getSource, SOURCE)
                        .eq(NewsFetchJobLog::getJobDate, targetDate)
                        .eq(NewsFetchJobLog::getTriggerType, TRIGGER_SCHEDULED)
                        .eq(NewsFetchJobLog::getStatus, STATUS_SUCCESS)
        );
        return count != null && count > 0;
    }

    /**
     * 将 Guardian 响应转换为新闻实体。
     *
     * @param item Guardian 单条结果
     * @param fetchDate 抓取日期
     * @return 新闻实体
     */
    private NewsArticle toNewsArticle(GuardianSearchResponse.ResultItem item, LocalDate fetchDate) {
        NewsArticle article = new NewsArticle();
        article.setSource(SOURCE);
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
        article.setFetchDate(fetchDate);
        article.setLang("en");
        article.setStatus(1);
        article.setRawJson(toRawJson(item));

        NewsClassifier.ClassificationResult classificationResult = newsClassifier.classify(
                article.getTitle(),
                article.getSummary(),
                article.getContent()
        );
        article.setCategoryCode(classificationResult.getCategoryCode());
        article.setCategoryName(classificationResult.getCategoryName());
        article.setKeywordTags(limit(classificationResult.getMatchedKeywords(), 500));
        return article;
    }

    /**
     * 按来源内容 ID 执行新增或更新。
     *
     * @param article 新闻实体
     * @return true 表示新增，false 表示更新
     */
    private boolean saveOrUpdateArticle(NewsArticle article) {
        NewsArticle existing = newsArticleMapper.selectOne(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getSource, article.getSource())
                        .eq(NewsArticle::getSourceContentId, article.getSourceContentId())
                        .last("LIMIT 1")
        );
        LocalDateTime now = LocalDateTime.now();
        article.setUpdatedAt(now);

        if (existing == null) {
            article.setCreatedAt(now);
            newsArticleMapper.insert(article);
            return true;
        }

        article.setId(existing.getId());
        article.setCreatedAt(existing.getCreatedAt());
        newsArticleMapper.updateById(article);
        return false;
    }

    /**
     * 解析发布时间。
     *
     * @param value 原始发布时间
     * @param fallbackDate 兜底日期
     * @return 解析后的时间
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
     * 序列化原始响应，便于排查问题。
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
