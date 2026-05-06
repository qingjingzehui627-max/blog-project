package com.example.blog.job;

import com.example.blog.config.NewsSummaryProperties;
import com.example.blog.service.NewsAiSummaryService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 新闻 AI 摘要定时任务。
 */
@Component
public class NewsAiSummaryJob {

    @Resource
    private NewsSummaryProperties newsSummaryProperties;

    @Resource
    private NewsAiSummaryService newsAiSummaryService;

    /**
     * 按配置时间执行每日前十条新闻自动摘要。
     */
    @Scheduled(cron = "${ai.news-summary.cron:0 40 6 * * ?}", zone = "${ai.news-summary.zone:Asia/Shanghai}")
    public void generateDailySummary() {
        if (!newsSummaryProperties.isEnabled() || !newsSummaryProperties.isAutoEnabled()) {
            return;
        }
        newsAiSummaryService.generateDailyTopSummaries();
    }
}
