package com.example.blog.job;

import com.example.blog.config.GuardianProperties;
import com.example.blog.service.GuardianNewsFetchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * Guardian 新闻定时抓取任务。
 */
@Component
public class GuardianNewsFetchJob {

    @Resource
    private GuardianProperties guardianProperties;

    @Resource
    private GuardianNewsFetchService guardianNewsFetchService;

    /**
     * 按配置时间执行新闻抓取。
     */
    @Scheduled(cron = "${guardian.cron:0 10 6 * * ?}", zone = "${guardian.zone:Asia/Shanghai}")
    public void fetchDailyNews() {
        // 根据偏移量计算本次需要抓取的新闻日期
        LocalDate targetDate = LocalDate.now().plusDays(guardianProperties.getFetchOffsetDays());
        guardianNewsFetchService.fetchNews(targetDate, "SCHEDULED");
    }
}
