package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.client.provider.NewsProviderAdapter;
import com.example.blog.client.provider.dto.NewsProviderFetchResult;
import com.example.blog.config.GuardianProperties;
import com.example.blog.dto.NewsProviderStatusResponse;
import com.example.blog.entity.NewsFetchJobLog;
import com.example.blog.mapper.NewsFetchJobLogMapper;
import com.example.blog.service.GuardianNewsFetchService;
import com.example.blog.service.support.NewsAggregationService;
import com.example.blog.service.support.dto.NewsAggregationResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新闻抓取服务实现。
 */
@Service
public class GuardianNewsFetchServiceImpl implements GuardianNewsFetchService {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String TRIGGER_SCHEDULED = "SCHEDULED";

    @Resource
    private GuardianProperties guardianProperties;

    @Resource
    private NewsFetchJobLogMapper newsFetchJobLogMapper;

    @Resource
    private NewsAggregationService newsAggregationService;

    @Resource
    private List<NewsProviderAdapter> newsProviderAdapters;

    /**
     * 执行一次新闻抓取任务。
     *
     * @param fetchDate 抓取日期
     * @param triggerType 触发方式
     * @param providerCode 指定数据源编码，传空表示抓取全部启用数据源
     * @return 抓取结果
     */
    @Override
    public Map<String, Object> fetchNews(LocalDate fetchDate, String triggerType, String providerCode) {
        LocalDate targetDate = fetchDate != null ? fetchDate : LocalDate.now();
        String safeTriggerType = StringUtils.hasText(triggerType) ? triggerType : "MANUAL";
        List<NewsProviderAdapter> targetAdapters = selectTargetAdapters(providerCode);

        if (targetAdapters.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", STATUS_FAILED);
            result.put("fetchDate", targetDate);
            result.put("message", "未找到可执行的数据源");
            result.put("providerResults", List.of());
            return result;
        }

        int totalRequestCount = 0;
        int totalFetchedCount = 0;
        int totalInsertedCount = 0;
        int totalUpdatedCount = 0;
        int totalFailedCount = 0;
        List<Map<String, Object>> providerResults = new ArrayList<>();

        // 统一遍历所有目标数据源适配器，
        // 保证后续新增来源时不需要再改抓取主流程。
        for (NewsProviderAdapter providerAdapter : targetAdapters) {
            NewsFetchJobLog jobLog = createJobLog(providerAdapter.getProviderCode(), targetDate, safeTriggerType);

            String validationMessage = providerAdapter.validateConfig();
            if (StringUtils.hasText(validationMessage)) {
                providerResults.add(skipJob(jobLog, validationMessage));
                if (providerAdapter.isEnabled()) {
                    totalFailedCount++;
                }
                continue;
            }
            if (TRIGGER_SCHEDULED.equalsIgnoreCase(safeTriggerType)
                    && hasSuccessfulScheduledJob(providerAdapter.getProviderCode(), targetDate)) {
                providerResults.add(skipJob(jobLog, "当天的定时抓取已经成功执行，跳过重复任务"));
                continue;
            }

            try {
                NewsProviderFetchResult fetchResult = providerAdapter.fetchArticles(targetDate);
                // 适配器只负责“抓原始数据”，聚合、去重、排序统一交给聚合服务处理。
                NewsAggregationResult aggregationResult = newsAggregationService.aggregateArticles(
                        providerAdapter.getProviderCode(),
                        fetchResult.getArticles(),
                        targetDate,
                        guardianProperties.getTopLimit()
                );

                jobLog.setStatus(STATUS_SUCCESS);
                jobLog.setRequestCount(fetchResult.getRequestCount());
                jobLog.setFetchedCount(aggregationResult.getFetchedCount());
                jobLog.setInsertedCount(aggregationResult.getInsertedCount());
                jobLog.setUpdatedCount(aggregationResult.getUpdatedCount());
                jobLog.setFailedCount(aggregationResult.getSkippedCount());
                jobLog.setFinishedAt(LocalDateTime.now());
                newsFetchJobLogMapper.updateById(jobLog);

                totalRequestCount += fetchResult.getRequestCount();
                totalFetchedCount += aggregationResult.getFetchedCount();
                totalInsertedCount += aggregationResult.getInsertedCount();
                totalUpdatedCount += aggregationResult.getUpdatedCount();
                totalFailedCount += aggregationResult.getSkippedCount();
                providerResults.add(buildProviderResult(jobLog, null));
            } catch (Exception exception) {
                totalFailedCount++;
                jobLog.setStatus(STATUS_FAILED);
                jobLog.setFinishedAt(LocalDateTime.now());
                jobLog.setFailedCount(1);
                jobLog.setErrorMessage(limit(exception.getMessage(), 2000));
                newsFetchJobLogMapper.updateById(jobLog);
                providerResults.add(buildProviderResult(jobLog, exception.getMessage()));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", totalFailedCount > 0 && totalFetchedCount == 0 ? STATUS_FAILED : STATUS_SUCCESS);
        result.put("fetchDate", targetDate);
        result.put("requestCount", totalRequestCount);
        result.put("fetchedCount", totalFetchedCount);
        result.put("insertedCount", totalInsertedCount);
        result.put("updatedCount", totalUpdatedCount);
        result.put("failedCount", totalFailedCount);
        result.put("providerResults", providerResults);
        return result;
    }

    /**
     * 分页查询抓取任务日志。
     *
     * @param page 页码
     * @param size 每页数量
     * @return 抓取日志结果
     */
    @Override
    public Map<String, Object> getJobLogs(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        Long total = newsFetchJobLogMapper.selectCount(new LambdaQueryWrapper<>());
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
     * 查询所有数据源状态。
     *
     * @return 数据源状态列表
     */
    @Override
    public List<NewsProviderStatusResponse> getProviderStatuses() {
        return newsProviderAdapters.stream()
                .map(adapter -> {
                    NewsProviderStatusResponse response = new NewsProviderStatusResponse();
                    response.setProviderCode(adapter.getProviderCode());
                    response.setDisplayName(adapter.getDisplayName());
                    response.setEnabled(adapter.isEnabled());
                    String validationMessage = adapter.validateConfig();
                    response.setConfigValid(!StringUtils.hasText(validationMessage));
                    response.setMessage(StringUtils.hasText(validationMessage) ? validationMessage : "配置正常");
                    response.setConfig(adapter.getAdminConfig());
                    return response;
                })
                .toList();
    }

    /**
     * 根据数据源编码筛选目标适配器。
     *
     * @param providerCode 指定数据源编码
     * @return 目标适配器列表
     */
    private List<NewsProviderAdapter> selectTargetAdapters(String providerCode) {
        if (!StringUtils.hasText(providerCode)) {
            // 全量抓取时只执行已启用的数据源，保留禁用实现但不参与调度。
            return newsProviderAdapters.stream()
                    .filter(NewsProviderAdapter::isEnabled)
                    .toList();
        }
        return newsProviderAdapters.stream()
                .filter(adapter -> providerCode.equalsIgnoreCase(adapter.getProviderCode()))
                .toList();
    }

    /**
     * 创建一条抓取任务日志。
     *
     * @param source 数据源编码
     * @param targetDate 抓取日期
     * @param triggerType 触发方式
     * @return 任务日志
     */
    private NewsFetchJobLog createJobLog(String source, LocalDate targetDate, String triggerType) {
        NewsFetchJobLog jobLog = new NewsFetchJobLog();
        jobLog.setSource(source);
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
        return buildProviderResult(jobLog, message);
    }

    /**
     * 判断当天是否已有成功的定时任务。
     *
     * @param source 数据源编码
     * @param targetDate 抓取日期
     * @return true 表示已经成功执行
     */
    private boolean hasSuccessfulScheduledJob(String source, LocalDate targetDate) {
        Long count = newsFetchJobLogMapper.selectCount(
                new LambdaQueryWrapper<NewsFetchJobLog>()
                        .eq(NewsFetchJobLog::getSource, source)
                        .eq(NewsFetchJobLog::getJobDate, targetDate)
                        .eq(NewsFetchJobLog::getTriggerType, TRIGGER_SCHEDULED)
                        .eq(NewsFetchJobLog::getStatus, STATUS_SUCCESS)
        );
        return count != null && count > 0;
    }

    /**
     * 组装单个数据源的返回结果。
     *
     * @param jobLog 任务日志
     * @param message 附加说明
     * @return 返回结果
     */
    private Map<String, Object> buildProviderResult(NewsFetchJobLog jobLog, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("jobId", jobLog.getId());
        result.put("source", jobLog.getSource());
        result.put("status", jobLog.getStatus());
        result.put("fetchDate", jobLog.getJobDate());
        result.put("requestCount", jobLog.getRequestCount());
        result.put("fetchedCount", jobLog.getFetchedCount());
        result.put("insertedCount", jobLog.getInsertedCount());
        result.put("updatedCount", jobLog.getUpdatedCount());
        result.put("failedCount", jobLog.getFailedCount());
        result.put("message", StringUtils.hasText(message) ? message : jobLog.getErrorMessage());
        return result;
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
