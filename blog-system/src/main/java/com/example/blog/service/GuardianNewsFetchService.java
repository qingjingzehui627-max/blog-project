package com.example.blog.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Guardian 新闻抓取服务。
 */
public interface GuardianNewsFetchService {
    /**
     * 执行一次新闻抓取任务。
     *
     * @param fetchDate 抓取日期
     * @param triggerType 触发方式
     * @return 抓取结果
     */
    Map<String, Object> fetchNews(LocalDate fetchDate, String triggerType);

    /**
     * 分页查询抓取日志。
     *
     * @param page 页码
     * @param size 每页数量
     * @return 抓取日志结果
     */
    Map<String, Object> getJobLogs(int page, int size);
}
