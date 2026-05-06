package com.example.blog.service;

import com.example.blog.dto.NewsProviderStatusResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 新闻抓取服务。
 */
public interface GuardianNewsFetchService {
    /**
     * 执行一次新闻抓取任务。
     *
     * @param fetchDate 抓取日期
     * @param triggerType 触发方式
     * @param providerCode 指定数据源编码，传空表示抓取全部启用数据源
     * @return 抓取结果
     */
    Map<String, Object> fetchNews(LocalDate fetchDate, String triggerType, String providerCode);

    /**
     * 分页查询抓取任务日志。
     *
     * @param page 页码
     * @param size 每页数量
     * @return 抓取日志结果
     */
    Map<String, Object> getJobLogs(int page, int size);

    /**
     * 查询所有数据源状态。
     *
     * @return 数据源状态列表
     */
    List<NewsProviderStatusResponse> getProviderStatuses();
}
