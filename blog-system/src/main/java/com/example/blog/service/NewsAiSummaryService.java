package com.example.blog.service;

import com.example.blog.dto.NewsAiSummaryResponse;

import java.time.LocalDate;
import java.util.Map;

/**
 * 新闻 AI 摘要服务。
 */
public interface NewsAiSummaryService {

    /**
     * 查询指定新闻的 AI 摘要。
     *
     * @param articleId 新闻主键
     * @return AI 摘要
     */
    NewsAiSummaryResponse getSummaryByArticleId(Long articleId);

    /**
     * 手动生成单条新闻摘要。
     *
     * @param articleId 新闻主键
     * @param triggerType 触发方式
     * @param force 是否强制重生成
     * @return 生成结果
     */
    Map<String, Object> generateSummary(Long articleId, String triggerType, boolean force);

    /**
     * 生成指定日期前十条热度新闻摘要。
     *
     * @param fetchDate 新闻抓取日期
     * @param triggerType 触发方式
     * @param force 是否强制重生成
     * @return 批量生成结果
     */
    Map<String, Object> generateTopSummaries(LocalDate fetchDate, String triggerType, boolean force);

    /**
     * 查询每日摘要额度。
     *
     * @param date 统计日期
     * @return 额度信息
     */
    Map<String, Object> getDailyQuota(LocalDate date);

    /**
     * 执行每日自动摘要任务。
     */
    void generateDailyTopSummaries();
}
